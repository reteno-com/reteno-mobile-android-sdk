package com.reteno.core.internal.lifecycle

import com.reteno.core.domain.model.event.LifecycleEvent
import com.reteno.core.internal.action.event.LogEvent
import com.reteno.core.internal.action.trace.ActionTrace

internal class LifecycleEvents(
    private val filter: LifecycleEventFilter,
    private val logEvent: LogEvent
) {

    suspend fun log(trace: ActionTrace, lifecycleEvent: LifecycleEvent) {
        val key = lifecycleEvent.event.eventTypeKey
        if (!filter.isEnabled(lifecycleEvent.type)) {
            trace.note { "$key event disabled by lifecycle tracking options" }
            return
        }
        trace.step("log $key event") { logEvent(lifecycleEvent.event) }
    }
}
