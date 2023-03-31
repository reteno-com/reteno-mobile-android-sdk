package com.reteno.core.data.repository

import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User

interface ContactRepository {
    fun saveUserData(user: User, toParallelWork: Boolean = true)
    fun saveDeviceData(device: Device, toParallelWork: Boolean = true)

    fun pushDeviceData()
    fun pushUserData()
}