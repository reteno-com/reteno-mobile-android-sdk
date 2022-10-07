package com.reteno.di.provider

import com.reteno.di.base.ProviderWeakReference
import com.reteno.domain.controller.EventController

class EventsControllerProvider(private val eventsRepositoryProvider: EventsRepositoryProvider) :
    ProviderWeakReference<EventController>() {

    override fun create(): EventController {
        return EventController(eventsRepositoryProvider.get())
    }
}