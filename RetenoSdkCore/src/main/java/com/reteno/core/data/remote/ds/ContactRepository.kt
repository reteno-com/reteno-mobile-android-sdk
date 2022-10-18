package com.reteno.core.data.remote.ds

import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device

interface ContactRepository {
    fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback)
}