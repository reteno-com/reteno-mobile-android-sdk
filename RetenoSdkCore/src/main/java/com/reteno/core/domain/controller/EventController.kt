package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.Event
import com.reteno.core.model.Events
import com.reteno.core.model.Parameter
import com.reteno.core.util.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

class EventController(
    private val eventsRepository: EventsRepository
) {

    /**
     * Attempt to send saved events
     */
    fun pushEvents() {
        val events = getFaceEvents() /* DB.getEvents() */
        eventsRepository.sendOutcomeEvent(events, object : ResponseCallback {

            override fun onSuccess(response: String) {
                val lastSentEventTime =
                    events.eventList.last().occurred.toEpochSecond(ZoneOffset.UTC) // TODO temporary solution
                clearEventsUntilDate(lastSentEventTime)
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                Logger.d("EventController", "pushEvents", statusCode, response, throwable)
            }

        })
    }

    /**
     * Deletes events older than exceptionDate.
     * @param expirationDate - the storage period of events in millis
     */
    fun clearOldEvents(expirationDate: Long) {
        // TODO DB.clearOldest24Hours()
    }

    /**
     *  Clear all events
     */
    fun clearAllEvents() {
        // TODO DB.clearAll()
    }

    fun clearEventsUntilDate(lastSentEventTime: Long) {
        // TODO DB.clearUntilDate(lastSentEventTime)
    }


    // ===== TODO only for test ======
    private fun getFaceEvents(): Events {
        return Events(
            "deviceId",
            "1",
            eventList = listOf(
                Event(
                    "Notify open",
                    LocalDateTime.now(),
                    params = listOf(
                        Parameter("param1", "1"),
                        Parameter("param2", "false")
                    )
                )
            )
        )
    }


}