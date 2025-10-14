package com.reteno.core.di.provider

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.di.base.ProviderWeakReference

internal class RestConfigProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val deviceIdHelperProvider: DeviceIdHelperProvider
) : ProviderWeakReference<RestConfig>() {

    override fun create(): RestConfig {
        return RestConfig(
            sharedPrefsManager = sharedPrefsManagerProvider.get(),
            deviceIdHelper = deviceIdHelperProvider.get()
        )
    }
}