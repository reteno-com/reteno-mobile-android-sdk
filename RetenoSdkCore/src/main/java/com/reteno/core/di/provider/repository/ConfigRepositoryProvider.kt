package com.reteno.core.di.provider.repository

import android.content.Context
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ConfigRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RestConfigProvider
import com.reteno.core.di.provider.SharedPrefsManagerProvider

internal class ConfigRepositoryProvider(
    private val context: Context,
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val restConfigProvider: RestConfigProvider
) :
    ProviderWeakReference<ConfigRepository>() {

    override fun create(): ConfigRepository {
        return ConfigRepositoryImpl(
            context,
            sharedPrefsManagerProvider.get(),
            restConfigProvider.get()
        )
    }
}