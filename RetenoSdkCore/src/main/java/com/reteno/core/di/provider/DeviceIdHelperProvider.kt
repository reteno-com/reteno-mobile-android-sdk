package com.reteno.core.di.provider

import android.content.Context
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.di.base.ProviderWeakReference

internal class DeviceIdHelperProvider(
    private val context: Context,
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val configProvider: RetenoConfigProvider
) : ProviderWeakReference<DeviceIdHelper>() {

    override fun create(): DeviceIdHelper {
        return DeviceIdHelper(
            context = context,
            sharedPrefsManager = sharedPrefsManagerProvider.get(),
            userIdProvider = configProvider.get().userIdProvider
        )
    }
}