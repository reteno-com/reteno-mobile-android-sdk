package com.reteno.core.di.provider

import com.reteno.core.data.remote.ds.EventsRepository
import com.reteno.core.data.remote.ds.EventsRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class EventsRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<EventsRepository>() {

    override fun create(): EventsRepository {
        return EventsRepositoryImpl(apiClientProvider.get())
    }
}