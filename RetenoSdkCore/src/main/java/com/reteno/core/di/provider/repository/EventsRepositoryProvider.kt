package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.data.repository.EventsRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerEventsProvider
import com.reteno.core.di.provider.network.ApiClientProvider

class EventsRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val retenoDatabaseManagerEventsProvider: RetenoDatabaseManagerEventsProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider
) : ProviderWeakReference<EventsRepository>() {

    override fun create(): EventsRepository {
        return EventsRepositoryImpl(
            apiClientProvider.get(),
            retenoDatabaseManagerEventsProvider.get(),
            configRepositoryProvider.get()
        )
    }
}