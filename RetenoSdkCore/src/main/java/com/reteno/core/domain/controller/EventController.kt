package com.reteno.core.domain.controller

import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.data.repository.LogEventRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.util.Logger

internal class EventController(
    private val eventsRepository: EventsRepository,
    private val logEventRepository: LogEventRepository
) {

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