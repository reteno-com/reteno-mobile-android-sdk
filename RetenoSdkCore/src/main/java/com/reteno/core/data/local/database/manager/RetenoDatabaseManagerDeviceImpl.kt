package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.database.util.getDevice
import com.reteno.core.data.local.database.util.putDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb

internal class RetenoDatabaseManagerDeviceImpl(private val database: RetenoDatabase) : RetenoDatabaseManagerDevice {

    override fun insertDevice(device: DeviceDb) {
        database.insert(table = DeviceSchema.TABLE_NAME, contentValues = ContentValues().apply { putDevice(device) })
    }

    override fun getDevices(limit: Int?): List<DeviceDb> = database.readRows(
        table = DeviceSchema.TABLE_NAME,
        rowIdColumn = DeviceSchema.COLUMN_DEVICE_ROW_ID,
        query = {
            query(
                table = DeviceSchema.TABLE_NAME,
                columns = DeviceSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
        },
        read = { getDevice() }
    )

    override fun getUnSyncedDeviceCount(): Long = database.getRowCount(
        DeviceSchema.TABLE_NAME,
        whereClause = "${DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}<>?",
        whereArgs = arrayOf(BooleanDb.TRUE.toString())
    )

    override fun deleteDevice(device: DeviceDb): Boolean = deleteRow(device.rowId) > 0

    override fun deleteDevices(devices: List<DeviceDb>) {
        devices.mapNotNull { it.rowId }.forEach { deleteRow(it) }
    }

    private fun deleteRow(rowId: String?): Int = database.delete(
        table = DeviceSchema.TABLE_NAME,
        whereClause = "${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
        whereArgs = arrayOf(rowId)
    )
}
