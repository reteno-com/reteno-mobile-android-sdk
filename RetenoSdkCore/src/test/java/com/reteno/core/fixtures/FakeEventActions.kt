package com.reteno.core.fixtures

import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.event.EventActions

internal class FakeEventActions(private val log: FakeCallLog = FakeCallLog()) : EventActions {
    val ecomEvents = mutableListOf<EcomEvent>()

    override fun submitEvent(event: Event) {
        log.record("submitEvent ${event.eventTypeKey}")
    }

    override fun submitEcomEvent(event: EcomEvent) {
        log.record("submitEcomEvent")
        ecomEvents += event
    }

    override fun submitScreenView(screenName: String) {
        log.record("submitScreenView $screenName")
    }
}
