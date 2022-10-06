package com.reteno.domain.controller

import com.reteno.data.remote.ds.EventsDataSource
import com.reteno.domain.ResponseCallback
import com.reteno.model.Event
import com.reteno.model.Events
import com.reteno.util.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

class EventController(
    private val eventsDataSource: EventsDataSource
) {

    /**
     * Attempt to send saved events
     */
    fun pushEvents() {
        val events = getFaceEvents() /* DB.getEvents() */
        eventsDataSource.sendOutcomeEvent(events, object : ResponseCallback {

            override fun onSuccess(response: String) {
                val lastSentEventTime = events.events.last().occurred.toEpochSecond(ZoneOffset.UTC) // TODO temporary solution
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
            events = listOf(
                Event(
                    "Notify open",
                    LocalDateTime.now(),
                    params = mapOf(
                        "param1" to 1,
                        "param2" to false
                    )
                )
            )
        )
    }


}