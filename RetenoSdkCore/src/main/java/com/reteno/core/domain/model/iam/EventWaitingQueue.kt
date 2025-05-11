package com.reteno.core.domain.model.iam

import com.reteno.core.domain.model.event.Event
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class EventWaitingQueue {
    private val eventQueue: ArrayDeque<EventWaitingForInApp> = ArrayDeque()
    private val acceptingEvents: AtomicBoolean = AtomicBoolean(true)

    fun pushEvent(event: Event) {
        if (!acceptingEvents.get()) return

        eventQueue.addLast(EventWaitingForInApp(System.currentTimeMillis(), event))
    }

    fun close() {
        acceptingEvents.set(false)
    }

    fun clear() {
        close()
        eventQueue.clear()
    }

    fun getEvents(): List<Event> {
        close()
        val discardBeforeMillis = System.currentTimeMillis() - EVENT_DISCARD_TIME
        val withoutOutdatedEvents = eventQueue.filter { it.waitingStartMillis > discardBeforeMillis }
        return withoutOutdatedEvents.map { it.event }
    }

    companion object {
        private val EVENT_DISCARD_TIME = 5.minutes.inWholeMilliseconds
    }
}