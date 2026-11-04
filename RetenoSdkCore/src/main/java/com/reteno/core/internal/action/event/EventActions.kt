package com.reteno.core.internal.action.event

import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event

internal interface EventActions {
    fun submitEvent(event: Event)

    fun submitEcomEvent(event: EcomEvent)

    fun submitScreenView(screenName: String)
}
