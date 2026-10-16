package com.reteno.core.di

import com.reteno.core.di.graph.DeeplinkGraph
import com.reteno.core.di.graph.DeviceGraph
import com.reteno.core.di.graph.EventGraph
import com.reteno.core.di.graph.InAppGraph
import com.reteno.core.di.graph.InboxGraph
import com.reteno.core.di.graph.InitGraph
import com.reteno.core.di.graph.InteractionGraph
import com.reteno.core.di.graph.LifecycleGraph
import com.reteno.core.di.graph.PushGraph
import com.reteno.core.di.graph.RecommendationGraph
import com.reteno.core.di.graph.SyncGraph
import com.reteno.core.di.graph.UserGraph
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.platform.AppBroadcasts
import com.reteno.core.internal.platform.AppEnvironment
import com.reteno.core.internal.platform.BackgroundWork
import com.reteno.core.internal.platform.HostUi
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope

internal class ActionGraph(
    infrastructure: SdkInfrastructure,
    dispatchers: RetenoDispatchers,
    logSink: RetenoLogSink,
    scope: CoroutineScope,
    configuration: SdkConfiguration,
    environment: AppEnvironment,
    broadcasts: AppBroadcasts,
    hostUi: HostUi,
    backgroundWork: BackgroundWork,
    lifecycleEventFilter: LifecycleEventFilter,
    clock: () -> Long
) {

    val router = SdkActionsRouter(
        graph = this,
        infrastructure = infrastructure,
        scope = scope,
        dispatchers = dispatchers,
        logSink = logSink
    )

    val core = ActionCore(
        infrastructure = infrastructure,
        dispatchers = dispatchers,
        logSink = logSink,
        scope = scope,
        configuration = configuration,
        environment = environment,
        broadcasts = broadcasts,
        hostUi = hostUi,
        backgroundWork = backgroundWork,
        lifecycleEventFilter = lifecycleEventFilter,
        clock = clock,
        logEventQueue = router
    )

    val events by lazy { EventGraph(core, infrastructure) }
    val device by lazy { DeviceGraph(core, infrastructure, this) }
    val user by lazy { UserGraph(core, infrastructure, this) }
    val interactions by lazy { InteractionGraph(core, infrastructure, this) }
    val deeplink by lazy { DeeplinkGraph(core, infrastructure, this) }
    val push by lazy { PushGraph(core, infrastructure, this) }
    val inbox by lazy { InboxGraph(core, infrastructure, this) }
    val recommendation by lazy { RecommendationGraph(core, infrastructure) }
    val inApp by lazy { InAppGraph(core, infrastructure, this) }
    val lifecycle by lazy { LifecycleGraph(core, infrastructure, this) }
    val init by lazy { InitGraph(core, infrastructure, this) }
    val sync by lazy { SyncGraph(core, infrastructure, this) }

    fun installLogging() {
        Logger.install(core.logSink, core.remoteLog)
    }
}
