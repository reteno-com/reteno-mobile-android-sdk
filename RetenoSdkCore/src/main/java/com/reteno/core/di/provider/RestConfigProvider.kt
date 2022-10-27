package com.reteno.core.di.provider

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.di.base.ProviderWeakReference

class RestConfigProvider(private val deviceIdHelperProvider: DeviceIdHelperProvider) :
    ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        return RestConfig(deviceIdHelperProvider.get())
    }
}