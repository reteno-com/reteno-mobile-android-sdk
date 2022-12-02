package com.reteno.core.data.repository

import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import java.time.ZonedDateTime

internal interface EventsRepository {

    fun saveEvent(event: Event)
    fun saveEcomEvent(ecomEvent: EcomEvent)
    fun pushEvents()
    fun clearOldEvents(outdatedTime: ZonedDateTime)

}