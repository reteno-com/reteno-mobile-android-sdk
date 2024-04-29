package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.identification.DeviceIdProvider

internal class DeviceIdHelperProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val userIdProvider: DeviceIdProvider?
) :
    ProviderWeakReference<DeviceIdHelper>() {

    override fun create(): DeviceIdHelper {
        return DeviceIdHelper(sharedPrefsManagerProvider.get(), userIdProvider)
    }
}