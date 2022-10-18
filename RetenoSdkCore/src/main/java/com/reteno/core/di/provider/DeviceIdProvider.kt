package com.reteno.core.di.provider

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.di.base.ProviderWeakReference

class DeviceIdProvider(private val sharedPrefsManagerProvider: SharedPrefsManagerProvider) :
    ProviderWeakReference<DeviceId>() {

    override fun create(): DeviceId {
        return DeviceId(sharedPrefsManagerProvider.get())
    }
}