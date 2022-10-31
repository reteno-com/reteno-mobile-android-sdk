package com.reteno.core.data.local.database

import com.reteno.core.model.device.Device

interface RetenoDatabaseManager {

    fun insertDevice(device: Device)
    fun getDeviceEvents(limit: Int?): List<Pair<String, Device>>
    fun getDeviceEventsCount(): Long
    fun deleteDeviceEvents(count: Int, ascending: Boolean = true)
}