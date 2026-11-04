package com.reteno.core.internal.action.event

import com.reteno.core.domain.model.event.Event

internal interface LogScreenViewEvent {
    suspend operator fun invoke(screenName: String): Result<Unit>
}

internal class LogScreenViewEventAction(
    private val logEvent: LogEvent
) : LogScreenViewEvent {

    override suspend operator fun invoke(screenName: String): Result<Unit> = logEvent(Event.ScreenView(screenName))
}
