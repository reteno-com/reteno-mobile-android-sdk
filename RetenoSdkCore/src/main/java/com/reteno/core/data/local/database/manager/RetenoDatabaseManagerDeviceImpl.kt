package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.database.util.getDevice
import com.reteno.core.data.local.database.util.putDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerDeviceImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerDevice {


    override fun insertDevice(device: DeviceDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertDevice(): ", "device = [", device, "]")
        /*@formatter:on*/

        val contentValues = ContentValues()
        contentValues.putDevice(device)
        database.insert(table = DeviceSchema.TABLE_NAME_DEVICE, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getDevices(limit: Int?): List<DeviceDb> {
        /*@formatter:off*/ Logger.i(TAG, "getDevices(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val deviceEvents: MutableList<DeviceDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = DeviceSchema.TABLE_NAME_DEVICE,
                columns = DeviceSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val device = cursor.getDevice()

                if (device != null) {
                    deviceEvents.add(device)
                } else {
                    val rowId = cursor.getLongOrNull(cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, device=$device")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getDevices(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = DeviceSchema.TABLE_NAME_DEVICE,
                            whereClause = "${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getDevices(). Removed invalid entry from database. device=$device ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Device from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return deviceEvents
    }

    override fun getUnSyncedDeviceCount(): Long = database.getRowCount(
        DeviceSchema.TABLE_NAME_DEVICE,
        whereClause = "${DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}<>?",
        whereArgs = arrayOf(BooleanDb.TRUE.toString())
    )

    override fun deleteDevice(device: DeviceDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteDevice(): ", "device = [", device, "]")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = DeviceSchema.TABLE_NAME_DEVICE,
            whereClause = "${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
            whereArgs = arrayOf(device.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteDevices(devices: List<DeviceDb>) {
        /*@formatter:off*/ Logger.i(TAG, "deleteDevices(): ", "devices: [", devices, "]")
        /*@formatter:on*/

        val rowIds = devices.mapNotNull { it.rowId }

        for (rowId: String in rowIds) {
            database.delete(
                table = DeviceSchema.TABLE_NAME_DEVICE,
                whereClause = "${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerDeviceImpl::class.java.simpleName
    }
}