package com.reteno.core.data.repository

import com.reteno.core.model.event.Event

interface EventsRepository {

    fun saveEvent(event: Event)
    fun pushEvents()

}