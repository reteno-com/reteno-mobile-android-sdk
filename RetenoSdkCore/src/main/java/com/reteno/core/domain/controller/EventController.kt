package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.model.event.Event
import com.reteno.core.util.Logger

class EventController(
    private val eventsRepository: EventsRepository
) {

    fun saveEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "saveEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        eventsRepository.saveEvent(event)
    }

    fun pushEvents() {
        /*@formatter:off*/ Logger.i(TAG, "pushEvents(): ", "")
        /*@formatter:on*/
        eventsRepository.pushEvents()
    }

    companion object {
        private val TAG: String = EventController::class.java.simpleName
    }
}