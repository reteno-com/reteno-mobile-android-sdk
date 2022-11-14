package com.reteno.core.data.repository

import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User

interface ContactRepository {
    fun saveUserData(user: User)
    fun saveDeviceData(device: Device)

    fun pushDeviceData()
    fun pushUserData()
}