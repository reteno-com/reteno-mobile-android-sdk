package com.reteno.core.data.remote.ds

import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device

internal class ContactRepositoryImpl(private val apiClient: ApiClient) : ContactRepository {
    override fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.Device, device.toJson(), responseHandler)
    }
}