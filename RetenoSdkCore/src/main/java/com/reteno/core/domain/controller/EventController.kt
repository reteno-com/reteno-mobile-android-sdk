package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.util.Logger

internal class EventController(
    private val eventsRepository: EventsRepository
) {

    private var iamController: IamController? = null // TODO debugging solution, this should not be here

    fun setIamController(iamController: IamController) {
        this.iamController = iamController
    }

    fun trackEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "trackEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        eventsRepository.saveEvent(event)
        iamController?.notifyEventOccurred(event)
    }

    fun trackScreenViewEvent(screenName: String) {
        /*@formatter:off*/ Logger.i(TAG, "trackScreenViewEvent(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        val event = Event.ScreenView(screenName)
        eventsRepository.saveEvent(event)
        iamController?.notifyEventOccurred(event)
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