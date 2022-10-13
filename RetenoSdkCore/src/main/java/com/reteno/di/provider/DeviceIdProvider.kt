package com.reteno.di.provider

import com.reteno.config.DeviceId
import com.reteno.di.base.ProviderWeakReference

class DeviceIdProvider(private val sharedPrefsManagerProvider: SharedPrefsManagerProvider) :
    ProviderWeakReference<DeviceId>() {

    override fun create(): DeviceId {
        return DeviceId(sharedPrefsManagerProvider.get())
    }
}