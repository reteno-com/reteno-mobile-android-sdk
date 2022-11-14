package com.reteno.core.data.repository

import com.reteno.core.domain.model.event.Event
import java.time.ZonedDateTime

interface EventsRepository {

    fun saveEvent(event: Event)
    fun pushEvents()
    fun clearOldEvents(outdatedTime: ZonedDateTime)

}