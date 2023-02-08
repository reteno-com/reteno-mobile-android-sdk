package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb

interface RetenoDatabaseManagerEvents {
    fun insertEvents(events: EventsDb)
    fun getEvents(limit: Int? = null): List<EventsDb>
    fun getEventsCount(): Long
    fun deleteEvents(count: Int, oldest: Boolean = true)
    fun deleteEventsByTime(outdatedTime: String): List<EventDb>
}