package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.device.DeviceDb

interface RetenoDatabaseManagerDevice {
    fun insertDevice(device: DeviceDb)
    fun getDevices(limit: Int? = null): List<DeviceDb>
    fun getDeviceCount(): Long
    fun deleteDevices(count: Int, oldest: Boolean = true)
}