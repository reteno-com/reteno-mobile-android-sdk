package com.reteno.di.provider

import com.reteno.data.local.config.RestConfig
import com.reteno.di.base.ProviderWeakReference

class RestConfigProvider(private val deviceIdProvider: DeviceIdProvider) :
    ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        return RestConfig(deviceIdProvider.get())
    }
}