package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.DbConstants.COLUMN_TIMESTAMP
import com.reteno.core.data.local.database.DbConstants.Device.TABLE_NAME_DEVICE
import com.reteno.core.data.local.database.DbUtil.getDevice
import com.reteno.core.data.local.database.DbUtil.putDevice
import com.reteno.core.model.device.Device
import com.reteno.core.util.Logger

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
                DbConstants.Device.getAllColumns(),
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP ASC",
                limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val device = cursor.getDevice()
                deviceEvents.add(timestamp to device)
            }
        } catch (t: Throwable) {
            handleSQLiteError("Unable to get events from the table.", t)
        } finally {
            cursor?.close()
        }
        return deviceEvents
    }

    override fun getDeviceEventsCount(): Long = databaseManager.getRowCount(TABLE_NAME_DEVICE)

    override fun deleteDeviceEvents(count: Int, ascending: Boolean) {
        val order = if (ascending) "ASC" else "DESC"
        databaseManager.delete(
            TABLE_NAME_DEVICE,
            "$COLUMN_TIMESTAMP in (select $COLUMN_TIMESTAMP from $TABLE_NAME_DEVICE ORDER BY $COLUMN_TIMESTAMP $order LIMIT $count)",
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