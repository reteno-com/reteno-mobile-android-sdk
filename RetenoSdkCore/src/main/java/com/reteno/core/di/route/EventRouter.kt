package com.reteno.core.di.route

import com.reteno.core.di.graph.EventGraph
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.action.event.EventActions
import com.reteno.core.internal.log.LogEventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class EventRouter(
    private val events: EventGraph,
    private val scope: CoroutineScope
) : EventActions, LogEventQueue {

    override fun submitEvent(event: Event) {
        scope.launch { events.logEvent(event) }
    }

    override fun submitEcomEvent(event: EcomEvent) {
        scope.launch { events.logEcomEvent(event) }
    }

    override fun submitScreenView(screenName: String) {
        scope.launch { events.logScreenView(screenName) }
    }

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        scope.launch { events.logRetenoEvent(event) }
    }
}
