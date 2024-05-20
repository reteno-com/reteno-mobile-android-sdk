package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.EventsRepositoryProvider
import com.reteno.core.di.provider.repository.LogEventRepositoryProvider
import com.reteno.core.domain.controller.EventController

internal class EventsControllerProvider(
    private val eventsRepositoryProvider: EventsRepositoryProvider,
    private val logEventRepository: LogEventRepositoryProvider
) : ProviderWeakReference<EventController>() {

    override fun create(): EventController {
        return EventController(eventsRepositoryProvider.get(), logEventRepository.get())
    }
}