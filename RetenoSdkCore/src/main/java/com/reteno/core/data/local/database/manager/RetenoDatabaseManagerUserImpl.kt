package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.UserSchema
import com.reteno.core.data.local.database.util.getUser
import com.reteno.core.data.local.database.util.putUser
import com.reteno.core.data.local.database.util.putUserAddress
import com.reteno.core.data.local.database.util.putUserAttributes
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerUserImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerUser {

    override fun insertUser(user: UserDb) {
        synchronized(this) {
            /*@formatter:off*/ Logger.i(TAG, "insertUser(): ", "user = [", user, "]")
            /*@formatter:on*/

            val contentValues = ContentValues()
            contentValues.putUser(user)
            val rowId = database.insert(table = UserSchema.TABLE_NAME_USER, contentValues = contentValues)
            contentValues.clear()

            user.userAttributes?.let { userAttrs ->
                contentValues.putUserAttributes(rowId, userAttrs)
                database.insert(table = UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES, contentValues = contentValues)
                contentValues.clear()

                userAttrs.address?.let { userAddress ->
                    contentValues.putUserAddress(rowId, userAddress)
                    database.insert(table = UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS, contentValues = contentValues)
                    contentValues.clear()
                }
            }
        }
    }

    override fun getUsers(limit: Int?): List<UserDb> {
        /*@formatter:off*/ Logger.i(TAG, "getUsers(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val userList: MutableList<UserDb> = mutableListOf()
        val rawQueryLimit: String = limit?.let { " LIMIT $it" } ?: ""

        var cursor: Cursor? = null
        try {
            val rawQuery = "SELECT" +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_USER_ROW_ID} AS ${UserSchema.COLUMN_USER_ROW_ID}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_DEVICE_ID} AS ${UserSchema.COLUMN_DEVICE_ID}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_EXTERNAL_USER_ID} AS ${UserSchema.COLUMN_EXTERNAL_USER_ID}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${DbSchema.COLUMN_TIMESTAMP} AS ${DbSchema.COLUMN_TIMESTAMP}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_SUBSCRIPTION_KEYS} AS ${UserSchema.COLUMN_SUBSCRIPTION_KEYS}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_GROUP_NAMES_INCLUDE} AS ${UserSchema.COLUMN_GROUP_NAMES_INCLUDE}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_GROUP_NAMES_EXCLUDE} AS ${UserSchema.COLUMN_GROUP_NAMES_EXCLUDE}," +
                    "  ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND} AS ${UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_PHONE} AS ${UserSchema.UserAttributesSchema.COLUMN_PHONE}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_EMAIL} AS ${UserSchema.UserAttributesSchema.COLUMN_EMAIL}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME} AS ${UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_LAST_NAME} AS ${UserSchema.UserAttributesSchema.COLUMN_LAST_NAME}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE} AS ${UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE} AS ${UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE}," +
                    "  ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS} AS ${UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS}," +
                    "  ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS}.${UserSchema.UserAddressSchema.COLUMN_REGION} AS ${UserSchema.UserAddressSchema.COLUMN_REGION}," +
                    "  ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS}.${UserSchema.UserAddressSchema.COLUMN_TOWN} AS ${UserSchema.UserAddressSchema.COLUMN_TOWN}," +
                    "  ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS}.${UserSchema.UserAddressSchema.COLUMN_ADDRESS} AS ${UserSchema.UserAddressSchema.COLUMN_ADDRESS}," +
                    "  ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS}.${UserSchema.UserAddressSchema.COLUMN_POSTCODE} AS ${UserSchema.UserAddressSchema.COLUMN_POSTCODE}" +
                    " FROM ${UserSchema.TABLE_NAME_USER}" +
                    "  LEFT JOIN ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES} ON ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_USER_ROW_ID} = ${UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES}.${UserSchema.COLUMN_USER_ROW_ID}" +
                    "  LEFT JOIN ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS} ON ${UserSchema.TABLE_NAME_USER}.${UserSchema.COLUMN_USER_ROW_ID} = ${UserSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS}.${UserSchema.COLUMN_USER_ROW_ID}" +
                    rawQueryLimit
            cursor = database.rawQuery(rawQuery)
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val user = cursor.getUser()

                if (user != null) {
                    userList.add(user)
                } else {
                    val rowId = cursor.getLongOrNull(cursor.getColumnIndex(UserSchema.COLUMN_USER_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, user=$user")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getUser(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = UserSchema.TABLE_NAME_USER,
                            whereClause = "${UserSchema.COLUMN_USER_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getUser(). Removed invalid entry from database. user=$user ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Users from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return userList
    }

    override fun getUnSyncedUserCount(): Long = database.getRowCount(
        UserSchema.TABLE_NAME_USER,
        whereClause = "${UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}<>?",
        whereArgs = arrayOf(BooleanDb.TRUE.toString())
    )

    override fun deleteUser(user: UserDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteUser(): ", "user = [", user, "]")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = UserSchema.TABLE_NAME_USER,
            whereClause = "${UserSchema.COLUMN_USER_ROW_ID}=?",
            whereArgs = arrayOf(user.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteUsers(users: List<UserDb>) {
        /*@formatter:off*/ Logger.i(TAG, "deleteUsers(): ", "users: [", users, "]")
        /*@formatter:on*/

        val rowIds = users.mapNotNull { it.rowId }

        for (rowId: String in rowIds) {
            database.delete(
                table = UserSchema.TABLE_NAME_USER,
                whereClause = "${UserSchema.COLUMN_USER_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerUserImpl::class.java.simpleName
    }
}