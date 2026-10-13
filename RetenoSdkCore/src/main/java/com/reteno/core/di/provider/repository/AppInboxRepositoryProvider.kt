package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.data.repository.AppInboxRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerAppInboxProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import com.reteno.core.internal.action.sync.DataSync

internal class AppInboxRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val retenoDatabaseManagerAppInboxProvider: RetenoDatabaseManagerAppInboxProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider,
    private val dataSync: () -> DataSync
) : ProviderWeakReference<AppInboxRepository>() {

    override fun create(): AppInboxRepository {
        return AppInboxRepositoryImpl(
            apiClientProvider.get(),
            retenoDatabaseManagerAppInboxProvider.get(),
            configRepositoryProvider.get(),
            requestSync = { dataSync().request() }
        )
    }
}