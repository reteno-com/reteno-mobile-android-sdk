package com.reteno.core.di.provider

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.di.base.ProviderWeakReference

class RestConfigProvider(private val deviceIdProvider: DeviceIdProvider) :
    ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        return RestConfig(deviceIdProvider.get())
    }
}