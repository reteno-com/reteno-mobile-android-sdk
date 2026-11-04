package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.EventRouter
import com.reteno.core.internal.action.event.LogEcomEventAction
import com.reteno.core.internal.action.event.LogEventAction
import com.reteno.core.internal.action.event.LogRetenoEventAction
import com.reteno.core.internal.action.event.LogScreenViewEventAction

internal class EventGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure
) {

    val logEvent: LogEventAction by lazy {
        LogEventAction(
            infrastructure.db.eventsDb,
            infrastructure.restConfig,
            core.eventBus,
            core.dispatchers,
            core.runner
        )
    }

    val logEcomEvent: LogEcomEventAction by lazy {
        LogEcomEventAction(
            infrastructure.db.eventsDb,
            infrastructure.restConfig,
            core.eventBus,
            core.dispatchers,
            core.runner
        )
    }

    val logScreenView: LogScreenViewEventAction by lazy {
        LogScreenViewEventAction(logEvent)
    }

    val logRetenoEvent: LogRetenoEventAction by lazy {
        LogRetenoEventAction(infrastructure.db.logEventsDb, core.dispatchers, core.runner)
    }

    val router: EventRouter by lazy {
        EventRouter(this, core.scope)
    }
}
