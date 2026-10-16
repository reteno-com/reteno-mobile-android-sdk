package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.LifecycleRouter
import com.reteno.core.internal.action.lifecycle.AppBackgroundedAction
import com.reteno.core.internal.action.lifecycle.AppForegroundedAction
import com.reteno.core.internal.action.lifecycle.EnableAutoScreenTrackingAction
import com.reteno.core.internal.action.lifecycle.SetLifecycleEventConfigAction
import com.reteno.core.internal.action.lifecycle.TrackPermissionDialogAction
import com.reteno.core.internal.action.lifecycle.UpdatePushPermissionStatusAction
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.lifecycle.AppForegroundState
import com.reteno.core.internal.lifecycle.AppLifecycleTracking
import com.reteno.core.internal.lifecycle.AppLifecycleTrackingImpl
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.core.internal.lifecycle.ScreenTracker

internal class LifecycleGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    private val lifecycleQueue = SerialQueue(core.scope, "Lifecycle", core.logSink)
    private val appForegroundState = AppForegroundState()

    val lifecycleEvents by lazy {
        LifecycleEvents(core.lifecycleEventFilter, graph.events.logEvent)
    }

    private val screenTracker by lazy {
        ScreenTracker(graph.events.logScreenView, core.scope)
    }

    private val appForegrounded: AppForegroundedAction by lazy {
        AppForegroundedAction(
            broadcasts = core.broadcasts,
            environment = core.environment,
            dataSync = graph.sync.dataSync,
            configRepository = infrastructure.configRepository,
            htmlPreload = graph.inApp.inAppHtmlPreload,
            state = appForegroundState,
            lifecycleEvents = lifecycleEvents,
            clock = core.clock,
            runner = core.runner
        )
    }

    private val appBackgrounded: AppBackgroundedAction by lazy {
        AppBackgroundedAction(infrastructure.sessionHandler, appForegroundState, lifecycleEvents, core.runner)
    }

    private val trackPermissionDialog: TrackPermissionDialogAction by lazy {
        TrackPermissionDialogAction(core.hostUi, lifecycleEvents, core.runner)
    }

    val lifecycleTracking: AppLifecycleTracking by lazy {
        AppLifecycleTrackingImpl(
            hostUi = core.hostUi,
            appForegrounded = appForegrounded,
            appBackgrounded = appBackgrounded,
            trackPermissionDialog = trackPermissionDialog,
            queue = lifecycleQueue,
            dispatchers = core.dispatchers
        )
    }

    val setLifecycleEventConfig: SetLifecycleEventConfigAction by lazy {
        SetLifecycleEventConfigAction(core.lifecycleEventFilter, core.runner)
    }

    val enableAutoScreenTracking: EnableAutoScreenTrackingAction by lazy {
        EnableAutoScreenTrackingAction(screenTracker, core.hostUi, core.runner)
    }

    val updatePushPermissionStatus: UpdatePushPermissionStatusAction by lazy {
        UpdatePushPermissionStatusAction(core.broadcasts, core.runner)
    }

    val router: LifecycleRouter by lazy {
        LifecycleRouter(core, this)
    }
}
