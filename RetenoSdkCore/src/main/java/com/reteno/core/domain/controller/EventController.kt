package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.model.event.Event
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.time.ZonedDateTime

class EventController(
    private val eventsRepository: EventsRepository
) {

    fun trackEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "trackEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        eventsRepository.saveEvent(event)
    }

    fun trackScreenViewEvent(screenName: String) {
        /*@formatter:off*/ Logger.i(TAG, "trackScreenViewEvent(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        eventsRepository.saveEvent(Event.ScreenView(screenName))
    }

    fun pushEvents() {
        /*@formatter:off*/ Logger.i(TAG, "pushEvents(): ", "")
        /*@formatter:on*/
        eventsRepository.pushEvents()
    }

    fun clearOldEvents() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "")
        /*@formatter:on*/
        val keepDataHours = if (Util.isDebugView()) {
            KEEP_EVENT_HOURS_DEBUG
        } else {
            KEEP_EVENT_HOURS
        }
        val outdatedTime = ZonedDateTime.now().minusHours(keepDataHours)
        eventsRepository.clearOldEvents(outdatedTime)
    }

    companion object {
        private val TAG: String = EventController::class.java.simpleName
        private const val KEEP_EVENT_HOURS = 24L
        private const val KEEP_EVENT_HOURS_DEBUG = 1L
    }
}