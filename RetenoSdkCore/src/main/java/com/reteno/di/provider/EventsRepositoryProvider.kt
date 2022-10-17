package com.reteno.di.provider

import com.reteno.data.remote.ds.EventsRepository
import com.reteno.data.remote.ds.EventsRepositoryImpl
import com.reteno.di.base.ProviderWeakReference

class EventsRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<EventsRepository>() {

    override fun create(): EventsRepository {
        return EventsRepositoryImpl(apiClientProvider.get())
    }
}