package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.lifecycle.AppForegroundState
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlin.time.Duration.Companion.milliseconds

internal interface AppBackgrounded {
    suspend operator fun invoke(): Result<Unit>
}

internal class AppBackgroundedAction(
    private val sessionHandler: RetenoSessionHandler,
    private val state: AppForegroundState,
    private val lifecycleEvents: LifecycleEvents,
    private val runner: ActionRunner
) : AppBackgrounded {

    override suspend operator fun invoke(): Result<Unit> = runner("AppBackgrounded") { trackBackgrounded() }

    private suspend fun ActionTrace.trackBackgrounded() {
        val openedAt = state.enterBackground()
        if (openedAt == null) {
            note { "app was not in foreground" }
            return
        }
        val seconds = step("read time spent in foreground", detail = { "seconds=$it" }) {
            sessionHandler.getForegroundTimeMillis().milliseconds.inWholeSeconds
        }
        lifecycleEvents.log(this, Event.applicationBackgrounded(openedAt, seconds))
    }
}
