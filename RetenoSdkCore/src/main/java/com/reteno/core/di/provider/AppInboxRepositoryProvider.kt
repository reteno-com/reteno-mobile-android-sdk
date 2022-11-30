package com.reteno.core.di.provider

import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.data.repository.AppInboxRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class AppInboxRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val databaseManagerProvider: DatabaseManagerProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider
) : ProviderWeakReference<AppInboxRepository>() {

    override fun create(): AppInboxRepository {
        return AppInboxRepositoryImpl(
            apiClientProvider.get(),
            databaseManagerProvider.get(),
            configRepositoryProvider.get()
        )
    }
}