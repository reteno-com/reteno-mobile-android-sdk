package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.util.Logger

internal class EventController(
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

    fun trackEcomEvent(ecomEvent: EcomEvent) {
        /*@formatter:off*/ Logger.i(TAG, "trackEcomEvent(): ", "event = [" , ecomEvent , "]")
        /*@formatter:on*/
        eventsRepository.saveEcomEvent(ecomEvent)
    }

    fun pushEvents() {
        /*@formatter:off*/ Logger.i(TAG, "pushEvents(): ", "")
        /*@formatter:on*/
        eventsRepository.pushEvents()
    }

    fun clearOldEvents() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "")
        /*@formatter:on*/
        val outdatedTime = SchedulerUtils.getOutdatedTime()
        eventsRepository.clearOldEvents(outdatedTime)
    }

    companion object {
        private val TAG: String = EventController::class.java.simpleName
    }
}