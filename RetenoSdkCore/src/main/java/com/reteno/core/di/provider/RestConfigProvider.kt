package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.di.base.ProviderWeakReference

internal class RestConfigProvider(
    private val deviceIdHelperProvider: DeviceIdHelperProvider,
    private val configProvider: RetenoConfigProvider
) : ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        val isUserProviderExist = configProvider.get().userIdProvider != null
        val deviceIdMode = if (isUserProviderExist) DeviceIdMode.CLIENT_UUID
        else DeviceIdMode.ANDROID_ID
        return RestConfig(deviceIdHelperProvider.get(), configProvider.get().accessKey, deviceIdMode)
    }
}