package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.EventsRepositoryProvider
import com.reteno.core.domain.controller.EventController

internal class EventsControllerProvider(private val eventsRepositoryProvider: EventsRepositoryProvider) :
    ProviderWeakReference<EventController>() {

    override fun create(): EventController {
        return EventController(eventsRepositoryProvider.get())
    }
}