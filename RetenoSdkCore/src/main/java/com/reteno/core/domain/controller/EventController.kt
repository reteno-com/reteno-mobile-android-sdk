package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.data.repository.LogEventRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.util.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal class EventController(
    private val eventsRepository: EventsRepository,
    private val logEventRepository: LogEventRepository
) {

    private val _eventFlow = MutableSharedFlow<Event>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventFlow: SharedFlow<Event> = _eventFlow

    fun trackEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "trackEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        eventsRepository.saveEvent(event)
        _eventFlow.tryEmit(event)
    }

    fun trackScreenViewEvent(screenName: String) {
        /*@formatter:off*/ Logger.i(TAG, "trackScreenViewEvent(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        val event = Event.ScreenView(screenName)
        eventsRepository.saveEvent(event)
        _eventFlow.tryEmit(event)
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

    fun trackRetenoEvent(event: RetenoLogEvent) {
        /*@formatter:off*/ Logger.i(TAG, "trackRetenoEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        logEventRepository.saveLogEvent(event)
    }

    companion object {
        private val TAG: String = EventController::class.java.simpleName
    }
}