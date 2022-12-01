package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ConfigRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RestConfigProvider
import com.reteno.core.di.provider.SharedPrefsManagerProvider

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