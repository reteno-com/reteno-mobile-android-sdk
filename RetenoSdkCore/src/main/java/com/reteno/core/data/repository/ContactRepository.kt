package com.reteno.core.data.repository

import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User

interface ContactRepository {
    fun saveUserData(user: User, toParallelWork: Boolean = false)
    fun saveDeviceData(device: Device, toParallelWork: Boolean = false)
    fun saveDeviceDataImmediate(device: Device)

    fun pushDeviceData()
    fun pushUserData()

    fun deleteSynchedDevices()
}