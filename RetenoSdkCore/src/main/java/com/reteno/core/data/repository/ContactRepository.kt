package com.reteno.core.data.repository

import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User

interface ContactRepository {
    @Deprecated("Use suspend alternative")
    fun saveUserDataDeprecated(user: User, toParallelWork: Boolean = false)
    suspend fun saveUserData(user: User)
    fun saveDeviceData(device: Device, toParallelWork: Boolean = false)
    fun saveDeviceDataImmediate(device: Device)
    suspend fun saveDeviceData(device: Device)

    fun pushDeviceData()
    fun pushUserData()

    fun pushDeviceDataImmediate()
    fun pushUserDataImmediate()

    fun deleteSynchedDevices()
}