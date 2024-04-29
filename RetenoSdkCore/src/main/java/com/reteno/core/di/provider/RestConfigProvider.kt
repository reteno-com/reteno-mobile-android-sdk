package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.di.base.ProviderWeakReference

internal class RestConfigProvider(
    private val deviceIdHelperProvider: DeviceIdHelperProvider,
    private val accessKey: String,
    private val isUserProviderExist: Boolean
) : ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        val deviceIdMode = if (isUserProviderExist) DeviceIdMode.CLIENT_UUID
        else DeviceIdMode.ANDROID_ID
        return RestConfig(deviceIdHelperProvider.get(), accessKey, deviceIdMode)
    }
}