package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.di.base.ProviderWeakReference

internal class DeviceIdHelperProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val configProvider: RetenoConfigProvider
) :
    ProviderWeakReference<DeviceIdHelper>() {

    override fun create(): DeviceIdHelper {
        return DeviceIdHelper(sharedPrefsManagerProvider.get(), configProvider.get().userIdProvider)
    }
}