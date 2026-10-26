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
import com.reteno.core.di.route.QueryRouter
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
        logEventQueue = lazy { events.router }
    )

    val events: EventGraph by lazy { EventGraph(core, infrastructure) }
    val device: DeviceGraph by lazy { DeviceGraph(core, infrastructure, this) }
    val user: UserGraph by lazy { UserGraph(core, infrastructure, this) }
    val interactions: InteractionGraph by lazy { InteractionGraph(core, infrastructure, this) }
    val deeplink: DeeplinkGraph by lazy { DeeplinkGraph(core, infrastructure, this) }
    val push: PushGraph by lazy { PushGraph(core, infrastructure, this) }
    val inbox: InboxGraph by lazy { InboxGraph(core, infrastructure, this) }
    val recommendation: RecommendationGraph by lazy { RecommendationGraph(core, infrastructure) }
    val inApp: InAppGraph by lazy { InAppGraph(core, infrastructure, this) }
    val lifecycle: LifecycleGraph by lazy { LifecycleGraph(core, infrastructure, this) }
    val init: InitGraph by lazy { InitGraph(core, infrastructure, this) }
    val sync: SyncGraph by lazy { SyncGraph(core, infrastructure, this) }

    val actions: SdkActions by lazy {
        SdkActionsImpl(
            control = init.router,
            queries = QueryRouter(infrastructure, core.remoteLog),
            contact = device.router,
            events = events.router,
            lifecycle = lifecycle.router,
            inApp = inApp.router,
            interactions = interactions.router,
            push = push.router,
            sync = sync.router,
            inbox = inbox.router,
            recommendation = recommendation.router
        )
    }

    fun installLogging() {
        Logger.install(core.logSink, core.remoteLog)
    }
}
