package com.reteno.core.di.provider

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.data.repository.EventsRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class EventsRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val retenoDatabaseManagerProvider: RetenoDatabaseManagerProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider
) : ProviderWeakReference<EventsRepository>() {

    override fun create(): EventsRepository {
        return EventsRepositoryImpl(
            apiClientProvider.get(),
            retenoDatabaseManagerProvider.get(),
            configRepositoryProvider.get()
        )
    }
}