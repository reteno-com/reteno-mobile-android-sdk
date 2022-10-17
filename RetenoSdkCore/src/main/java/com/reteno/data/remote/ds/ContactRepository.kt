package com.reteno.data.remote.ds

import com.reteno.domain.ResponseCallback
import com.reteno.model.device.Device

interface ContactRepository {
    fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback)
}