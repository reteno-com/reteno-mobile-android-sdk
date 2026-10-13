package com.reteno.core.internal.action.event

import com.reteno.core.domain.model.event.Event

internal class LogScreenViewEventAction(
    private val logEvent: LogEventAction
) {

    suspend operator fun invoke(screenName: String): Result<Unit> = logEvent(Event.ScreenView(screenName))
}
