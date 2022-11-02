package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getStringOrNull
import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.DbSchema.COLUMN_TIMESTAMP
import com.reteno.core.data.local.database.DbSchema.DeviceSchema.TABLE_NAME_DEVICE
import com.reteno.core.data.local.database.DbSchema.InteractionSchema.COLUMN_ID
import com.reteno.core.data.local.database.DbSchema.InteractionSchema.TABLE_NAME_INTERACTION
import com.reteno.core.data.local.database.DbSchema.UserAddressSchema.COLUMN_ADDRESS
import com.reteno.core.data.local.database.DbSchema.UserAddressSchema.COLUMN_POSTCODE
import com.reteno.core.data.local.database.DbSchema.UserAddressSchema.COLUMN_REGION
import com.reteno.core.data.local.database.DbSchema.UserAddressSchema.COLUMN_TOWN
import com.reteno.core.data.local.database.DbSchema.UserAddressSchema.TABLE_NAME_USER_ADDRESS
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_EMAIL
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_LAST_NAME
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_PHONE
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_TIME_CUSTOM_FIELDS
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE
import com.reteno.core.data.local.database.DbSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_DEVICE_ID
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS
import com.reteno.core.data.local.database.DbSchema.UserSchema.COLUMN_USER_ROW_ID
import com.reteno.core.data.local.database.DbSchema.UserSchema.TABLE_NAME_USER
import com.reteno.core.data.local.database.DbUtil.getDevice
import com.reteno.core.data.local.database.DbUtil.getInteraction
import com.reteno.core.data.local.database.DbUtil.getUser
import com.reteno.core.data.local.database.DbUtil.putDevice
import com.reteno.core.data.local.database.DbUtil.putInteraction
import com.reteno.core.data.local.database.DbUtil.putUser
import com.reteno.core.data.local.database.DbUtil.putUserAddress
import com.reteno.core.data.local.database.DbUtil.putUserAttributes
import com.reteno.core.data.local.model.InteractionModelDb
import com.reteno.core.data.remote.model.user.UserDTO
import com.reteno.core.model.device.Device
import com.reteno.core.util.Logger
import com.reteno.core.util.allElementsNotNull

// TODO: USE ENCRYPTION
//import net.sqlcipher.DatabaseUtils
//import net.sqlcipher.database.SQLiteDatabase

class RetenoDatabaseManagerImpl : RetenoDatabaseManager {

    private val databaseManager: RetenoDatabaseImpl by lazy {
//        SQLiteDatabase.loadLibs(RetenoImpl.application)
        RetenoDatabaseImpl(RetenoImpl.application)
    }
    private val contentValues = ContentValues()


    override fun insertDevice(device: Device) {
        contentValues.putDevice(device)
        databaseManager.insert(TABLE_NAME_DEVICE, null, contentValues)
        contentValues.clear()
    }

    override fun getDeviceEvents(limit: Int?): List<Pair<String, Device>> {
        val deviceEvents: MutableList<Pair<String, Device>> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = databaseManager.query(
                TABLE_NAME_DEVICE,
                DbSchema.DeviceSchema.getAllColumns(),
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP ASC",
                limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val device = cursor.getDevice()

                if (allElementsNotNull(timestamp, device)) {
                    deviceEvents.add(timestamp!! to device!!)
                } else {
                    val rowId = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_USER_ROW_ID))
                    val exception = SQLException("Unable to read data from SQL database. timeStamp=$timestamp, device=$device")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getUserEvents(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        databaseManager.delete(TABLE_NAME_USER, "$COLUMN_USER_ROW_ID=?", arrayOf(rowId))
                        /*@formatter:off*/ Logger.e(TAG, "getUserEvents(). Removed invalid entry from database. device=$device ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            handleSQLiteError("Unable to get events from the table.", t)
        } finally {
            cursor?.close()
        }
        return deviceEvents
    }

    override fun getDeviceEventsCount(): Long = databaseManager.getRowCount(TABLE_NAME_DEVICE)

    override fun deleteDeviceEvents(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        databaseManager.delete(
            TABLE_NAME_DEVICE,
            "$COLUMN_TIMESTAMP in (select $COLUMN_TIMESTAMP from $TABLE_NAME_DEVICE ORDER BY $COLUMN_TIMESTAMP $order LIMIT $count)",
            null
        )
    }


    //==============================================================================================
    override fun insertUser(user: UserDTO) {
        contentValues.putUser(user)
        val rowId = databaseManager.insert(TABLE_NAME_USER, null, contentValues)
        contentValues.clear()

        user.userAttributes?.let { userAttrs ->
            contentValues.putUserAttributes(rowId, userAttrs)
            databaseManager.insert(TABLE_NAME_USER_ATTRIBUTES, null, contentValues)
            contentValues.clear()

            userAttrs.address?.let { userAddress ->
                contentValues.putUserAddress(rowId, userAddress)
                databaseManager.insert(TABLE_NAME_USER_ADDRESS, null, contentValues)
                contentValues.clear()
            }
        }
    }

    override fun getUserEvents(limit: Int?): List<Pair<String, UserDTO>> {
        val userEvents: MutableList<Pair<String, UserDTO>> = mutableListOf()

        var cursor: Cursor? = null
        try {
            val rawQuery = "SELECT" +
                    "  $TABLE_NAME_USER.$COLUMN_DEVICE_ID AS $COLUMN_DEVICE_ID," +
                    "  $TABLE_NAME_USER.$COLUMN_EXTERNAL_USER_ID AS $COLUMN_EXTERNAL_USER_ID," +
                    "  $TABLE_NAME_USER.$COLUMN_TIMESTAMP AS $COLUMN_TIMESTAMP," +
                    "  $TABLE_NAME_USER.$COLUMN_SUBSCRIPTION_KEYS AS $COLUMN_SUBSCRIPTION_KEYS," +
                    "  $TABLE_NAME_USER.$COLUMN_GROUP_NAMES_INCLUDE AS $COLUMN_GROUP_NAMES_INCLUDE," +
                    "  $TABLE_NAME_USER.$COLUMN_GROUP_NAMES_EXCLUDE AS $COLUMN_GROUP_NAMES_EXCLUDE," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_PHONE AS $COLUMN_PHONE," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_EMAIL AS $COLUMN_EMAIL," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_FIRST_NAME AS $COLUMN_FIRST_NAME," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_LAST_NAME AS $COLUMN_LAST_NAME," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_LANGUAGE_CODE AS $COLUMN_LANGUAGE_CODE," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_TIME_ZONE AS $COLUMN_TIME_ZONE," +
                    "  $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_TIME_CUSTOM_FIELDS AS $COLUMN_TIME_CUSTOM_FIELDS," +
                    "  $TABLE_NAME_USER_ADDRESS.$COLUMN_REGION AS $COLUMN_REGION," +
                    "  $TABLE_NAME_USER_ADDRESS.$COLUMN_TOWN AS $COLUMN_TOWN," +
                    "  $TABLE_NAME_USER_ADDRESS.$COLUMN_ADDRESS AS $COLUMN_ADDRESS," +
                    "  $TABLE_NAME_USER_ADDRESS.$COLUMN_POSTCODE AS $COLUMN_POSTCODE" +
                    " FROM $TABLE_NAME_USER" +
                    "  LEFT JOIN $TABLE_NAME_USER_ATTRIBUTES ON $TABLE_NAME_USER.$COLUMN_USER_ROW_ID = $TABLE_NAME_USER_ATTRIBUTES.$COLUMN_USER_ROW_ID" +
                    "  LEFT JOIN $TABLE_NAME_USER_ADDRESS ON $TABLE_NAME_USER.$COLUMN_USER_ROW_ID = $TABLE_NAME_USER_ADDRESS.$COLUMN_USER_ROW_ID"
            cursor = databaseManager.rawQuery(rawQuery, null)
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val user = cursor.getUser()

                if (allElementsNotNull(timestamp, user)) {
                    userEvents.add(timestamp!! to user!!)
                } else {
                    val rowId = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_USER_ROW_ID))
                    val exception = SQLException("Unable to read data from SQL database. timeStamp=$timestamp, user=$user")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getUserEvents(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        databaseManager.delete(TABLE_NAME_USER, "$COLUMN_USER_ROW_ID=?", arrayOf(rowId))
                        /*@formatter:off*/ Logger.e(TAG, "getUserEvents(). Removed invalid entry from database. user=$user ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            handleSQLiteError("Unable to get events from the table.", t)
        } finally {
            cursor?.close()
        }
        return userEvents
    }

    override fun getUserEventsCount(): Long = databaseManager.getRowCount(TABLE_NAME_USER)

    override fun deleteUserEvents(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        databaseManager.delete(
            TABLE_NAME_USER,
            "$COLUMN_TIMESTAMP in (select $COLUMN_TIMESTAMP from $TABLE_NAME_USER ORDER BY $COLUMN_TIMESTAMP $order LIMIT $count)",
            null
        )
    }

    //==============================================================================================
    override fun insertInteraction(interaction: InteractionModelDb) {
        contentValues.putInteraction(interaction)
        databaseManager.insert(TABLE_NAME_INTERACTION, null, contentValues)
        contentValues.clear()
    }

    override fun getInteractionEvents(limit: Int?): List<Pair<String, InteractionModelDb>> {
        val interactionEvents: MutableList<Pair<String, InteractionModelDb>> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = databaseManager.query(
                TABLE_NAME_INTERACTION,
                DbSchema.InteractionSchema.getAllColumns(),
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP ASC",
                limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val interaction = cursor.getInteraction()

                if (allElementsNotNull(timestamp, interaction)) {
                    interactionEvents.add(timestamp!! to interaction!!)
                } else {
                    val rowId = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_ID))
                    val exception = SQLException("Unable to read data from SQL database. timeStamp=$timestamp, interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getInteractionEvents(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        databaseManager.delete(TABLE_NAME_USER, "$COLUMN_USER_ROW_ID=?", arrayOf(rowId))
                        /*@formatter:off*/ Logger.e(TAG, "getInteractionEvents(). Removed invalid entry from database. interaction=$interaction ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            handleSQLiteError("Unable to get events from the table.", t)
        } finally {
            cursor?.close()
        }
        return interactionEvents
    }

    override fun getInteractionEventsCount(): Long = databaseManager.getRowCount(TABLE_NAME_INTERACTION)

    override fun deleteInteractionEvents(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        databaseManager.delete(
            TABLE_NAME_INTERACTION,
            "$COLUMN_TIMESTAMP in (select $COLUMN_TIMESTAMP from $TABLE_NAME_INTERACTION ORDER BY $COLUMN_TIMESTAMP $order LIMIT $count)",
            null
        )
    }

    private fun handleSQLiteError(log: String, t: Throwable) {
        /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): $log", t)
        /*@formatter:on*/
    }


    companion object {
        val TAG: String = RetenoDatabaseManagerImpl::class.java.simpleName
    }
}