package com.reteno.di.provider

import com.reteno.data.local.ds.ConfigRepository
import com.reteno.di.base.ProviderWeakReference
import com.reteno.data.local.ds.ConfigRepositoryImpl

class ConfigRepositoryProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val restConfigProvider: RestConfigProvider
) :
    ProviderWeakReference<ConfigRepository>() {

    override fun create(): ConfigRepository {
        return ConfigRepositoryImpl(
            sharedPrefsManagerProvider.get(),
            restConfigProvider.get()
        )
    }
}