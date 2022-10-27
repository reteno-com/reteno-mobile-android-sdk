package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.di.base.ProviderWeakReference

class DeviceIdHelperProvider(private val sharedPrefsManagerProvider: SharedPrefsManagerProvider) :
    ProviderWeakReference<DeviceIdHelper>() {

    override fun create(): DeviceIdHelper {
        return DeviceIdHelper(sharedPrefsManagerProvider.get())
    }
}