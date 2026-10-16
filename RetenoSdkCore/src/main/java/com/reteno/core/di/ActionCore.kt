package com.reteno.core.di

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.RemoteLogFailureReporter
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.log.LogEventQueue
import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.internal.log.RemoteLogRecorder
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.internal.platform.AppBroadcasts
import com.reteno.core.internal.platform.AppEnvironment
import com.reteno.core.internal.platform.BackgroundWork
import com.reteno.core.internal.platform.HostUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import java.util.UUID

internal class ActionCore(
    infrastructure: SdkInfrastructure,
    val dispatchers: RetenoDispatchers,
    val logSink: RetenoLogSink,
    val scope: CoroutineScope,
    val configuration: SdkConfiguration,
    val environment: AppEnvironment,
    val broadcasts: AppBroadcasts,
    val hostUi: HostUi,
    val backgroundWork: BackgroundWork,
    val lifecycleEventFilter: LifecycleEventFilter,
    val clock: () -> Long,
    logEventQueue: LogEventQueue
) {

    val initGate = InitGate()
    val mainScope = scope + dispatchers.main
    val eventBus = EventBus()
    val newId: () -> String = { UUID.randomUUID().toString() }
    val remoteLog: RemoteLog = RemoteLogRecorder(
        configRepository = lazy { infrastructure.configRepository },
        environment = environment,
        queue = logEventQueue
    )
    val runner = ActionRunner(logSink, RemoteLogFailureReporter(remoteLog))
    val api by lazy { SuspendApiClient(infrastructure.restClient, dispatchers) }
}
