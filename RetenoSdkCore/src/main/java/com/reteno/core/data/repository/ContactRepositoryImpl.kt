package com.reteno.core.data.repository

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.model.user.User

internal class ContactRepositoryImpl(
    private val apiClient: ApiClient,
    private val restConfig: RestConfig
) : ContactRepository {
    override fun sendDeviceProperties(device: Device, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.Device, device.toJson(), responseHandler)
    }

    override fun sendUserData(user: User, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.User, user.toRemote(restConfig.deviceId).toJson(), responseHandler)
    }
}