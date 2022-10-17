package com.reteno.data.remote.ds

import com.reteno.data.remote.api.ApiClient
import com.reteno.data.remote.api.ApiContract
import com.reteno.data.remote.mapper.toJson
import com.reteno.domain.ResponseCallback
import com.reteno.model.device.Device

internal class ContactRepositoryImpl(private val apiClient: ApiClient) : ContactRepository {
    override fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.Device, device.toJson(), responseHandler)
    }
}