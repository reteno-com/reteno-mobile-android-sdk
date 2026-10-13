package com.reteno.core.di

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.internal.action.event.LogEcomEventAction
import com.reteno.core.internal.action.event.LogEventAction
import com.reteno.core.internal.action.event.LogRetenoEventAction
import com.reteno.core.internal.action.event.LogScreenViewEventAction
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.RemoteLogFailureReporter
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.log.RetenoLogSink

internal class ActionsLocator(
    private val serviceLocator: ServiceLocator,
    private val dispatchers: RetenoDispatchers,
    logSink: RetenoLogSink,
    captureRemoteLog: (String) -> Unit
) {

    private val runner = ActionRunner(logSink, RemoteLogFailureReporter(captureRemoteLog))
    private val eventBus = EventBus()
    private val deviceId: () -> DeviceId = { serviceLocator.restConfigProvider.get().deviceId }

    val logEvent: LogEventAction by lazy {
        LogEventAction(serviceLocator.retenoDatabaseManagerEventsProvider.get(), deviceId, eventBus, dispatchers, runner)
    }

    val logEcomEvent: LogEcomEventAction by lazy {
        LogEcomEventAction(serviceLocator.retenoDatabaseManagerEventsProvider.get(), deviceId, eventBus, dispatchers, runner)
    }

    val logScreenView: LogScreenViewEventAction by lazy {
        LogScreenViewEventAction(logEvent)
    }

    val logRetenoEvent: LogRetenoEventAction by lazy {
        LogRetenoEventAction(serviceLocator.retenoDatabaseManagerLogEventProvider.get(), dispatchers, runner)
    }
}
