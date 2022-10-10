package com.reteno.di.provider

import android.content.Context
import com.reteno.config.DeviceId
import com.reteno.di.base.ProviderWeakReference

class DeviceIdProvider(private val applicationContext: Context, private val sharedPrefsManagerProvider: SharedPrefsManagerProvider) :
    ProviderWeakReference<DeviceId>() {

    override fun create(): DeviceId {
        return DeviceId(applicationContext, sharedPrefsManagerProvider.get())
    }
}