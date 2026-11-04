package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.UserSchema
import com.reteno.core.data.local.database.schema.UserSchema.UserAddressSchema
import com.reteno.core.data.local.database.schema.UserSchema.UserAttributesSchema
import com.reteno.core.data.local.database.util.getUser
import com.reteno.core.data.local.database.util.putUser
import com.reteno.core.data.local.database.util.putUserAddress
import com.reteno.core.data.local.database.util.putUserAttributes
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserDb

internal class RetenoDatabaseManagerUserImpl(private val database: RetenoDatabase) : RetenoDatabaseManagerUser {

    override fun insertUser(user: UserDb) {
        synchronized(this) {
            val rowId = database.insert(
                table = UserSchema.TABLE_NAME,
                contentValues = ContentValues().apply { putUser(user) }
            )
            val attributes = user.userAttributes ?: return
            database.insert(
                table = UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES,
                contentValues = ContentValues().apply { putUserAttributes(rowId, attributes) }
            )
            val address = attributes.address ?: return
            database.insert(
                table = UserAddressSchema.TABLE_NAME_USER_ADDRESS,
                contentValues = ContentValues().apply { putUserAddress(rowId, address) }
            )
        }
    }

    override fun getUsers(limit: Int?): List<UserDb> = database.readRows(
        table = UserSchema.TABLE_NAME,
        rowIdColumn = UserSchema.COLUMN_USER_ROW_ID,
        query = { rawQuery(usersQuery(limit)) },
        read = { getUser() }
    )

    private fun usersQuery(limit: Int?): String {
        val user = UserSchema.TABLE_NAME
        val attributes = UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES
        val address = UserAddressSchema.TABLE_NAME_USER_ADDRESS
        val columns = listOf(
            user to UserSchema.COLUMN_USER_ROW_ID,
            user to UserSchema.COLUMN_DEVICE_ID,
            user to UserSchema.COLUMN_EXTERNAL_USER_ID,
            user to DbSchema.COLUMN_TIMESTAMP,
            user to UserSchema.COLUMN_SUBSCRIPTION_KEYS,
            user to UserSchema.COLUMN_GROUP_NAMES_INCLUDE,
            user to UserSchema.COLUMN_GROUP_NAMES_EXCLUDE,
            user to UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND,
            attributes to UserAttributesSchema.COLUMN_PHONE,
            attributes to UserAttributesSchema.COLUMN_EMAIL,
            attributes to UserAttributesSchema.COLUMN_FIRST_NAME,
            attributes to UserAttributesSchema.COLUMN_LAST_NAME,
            attributes to UserAttributesSchema.COLUMN_LANGUAGE_CODE,
            attributes to UserAttributesSchema.COLUMN_TIME_ZONE,
            attributes to UserAttributesSchema.COLUMN_CUSTOM_FIELDS,
            attributes to UserAttributesSchema.COLUMN_MARKET_ID,
            address to UserAddressSchema.COLUMN_REGION,
            address to UserAddressSchema.COLUMN_TOWN,
            address to UserAddressSchema.COLUMN_ADDRESS,
            address to UserAddressSchema.COLUMN_POSTCODE
        )
        val rowId = UserSchema.COLUMN_USER_ROW_ID
        return selectColumns(columns) +
            " FROM $user" +
            " LEFT JOIN $attributes ON $user.$rowId = $attributes.$rowId" +
            " LEFT JOIN $address ON $user.$rowId = $address.$rowId" +
            limitClause(limit)
    }

    override fun getUnSyncedUserCount(): Long = database.getRowCount(
        UserSchema.TABLE_NAME,
        whereClause = "${UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}<>?",
        whereArgs = arrayOf(BooleanDb.TRUE.toString())
    )

    override fun deleteUser(user: UserDb): Boolean = deleteRow(user.rowId) > 0

    override fun deleteUsers(users: List<UserDb>) {
        users.mapNotNull { it.rowId }.forEach { deleteRow(it) }
    }

    private fun deleteRow(rowId: String?): Int = database.delete(
        table = UserSchema.TABLE_NAME,
        whereClause = "${UserSchema.COLUMN_USER_ROW_ID}=?",
        whereArgs = arrayOf(rowId)
    )
}
