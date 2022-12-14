package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.database.util.getDevice
import com.reteno.core.data.local.database.util.putDevice
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.SQLException

internal class RetenoDatabaseManagerDeviceImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerDevice {

    private val contentValues = ContentValues()

    override fun insertDevice(device: DeviceDb) {
        contentValues.putDevice(device)
        database.insert(table = DeviceSchema.TABLE_NAME_DEVICE, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getDevices(limit: Int?): List<DeviceDb> {
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

    override fun getDeviceCount(): Long = database.getRowCount(DeviceSchema.TABLE_NAME_DEVICE)

    override fun deleteDevices(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        database.delete(
            table = DeviceSchema.TABLE_NAME_DEVICE,
            whereClause = "${DeviceSchema.COLUMN_DEVICE_ROW_ID} in (select ${DeviceSchema.COLUMN_DEVICE_ROW_ID} from ${DeviceSchema.TABLE_NAME_DEVICE} ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order LIMIT $count)"
        )
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerDeviceImpl::class.java.simpleName
    }
}