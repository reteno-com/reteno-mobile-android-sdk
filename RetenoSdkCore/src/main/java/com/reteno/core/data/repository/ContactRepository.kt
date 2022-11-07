package com.reteno.core.data.repository

import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.model.user.User

interface ContactRepository {
    fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback)
    fun sendUserData(user: User, responseHandler: ResponseCallback)
}