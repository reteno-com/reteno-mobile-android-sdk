package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.InitRouter
import com.reteno.core.internal.action.init.ApplyConfigAction
import com.reteno.core.internal.action.init.BootSdkAction
import com.reteno.core.internal.action.init.SetSessionDurationAction
import com.reteno.core.internal.action.init.StartSdkAction
import com.reteno.core.internal.action.init.StopSdkAction
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.internal.lifecycle.StartStopSequencer

internal class InitGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    val runState = SdkRunState()

    val initQueue by lazy { SerialQueue(core.mainScope, "Init", core.logSink) }

    val sdk by lazy {
        StartStopSequencer(core.mainScope, core.initGate, startSdk, stopSdk, runState)
    }

    val applyConfig: ApplyConfigAction by lazy {
        ApplyConfigAction(
            configuration = core.configuration,
            restConfig = infrastructure.restConfig,
            configRepository = infrastructure.configRepository,
            prefs = infrastructure.prefs,
            sessionHandler = infrastructure.sessionHandler,
            registerDevice = graph.device.registerDevice,
            initGate = core.initGate,
            pauseInApps = graph.inApp.pauseInApps,
            iamView = graph.inApp.inAppPresenter,
            lifecycleTracking = graph.lifecycle.lifecycleTracking,
            processCachedInteractions = graph.interactions.processCachedInteractions,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    val bootSdk: BootSdkAction by lazy {
        BootSdkAction(
            core.backgroundWork,
            infrastructure.prefs,
            infrastructure.restConfig,
            applyConfig,
            core.dispatchers,
            core.runner
        )
    }

    private val startSdk: StartSdkAction by lazy {
        StartSdkAction(
            ensureDeviceRegistered = graph.device.ensureDeviceRegisteredThisSession,
            sessionHandler = infrastructure.sessionHandler,
            fetchInAppMessages = graph.inApp.fetchInAppMessages,
            processInAppMessages = graph.inApp.processInAppMessages,
            dataSync = graph.sync.dataSync,
            iamView = graph.inApp.inAppPresenter,
            inboxCount = graph.inbox.inboxCountPoller,
            state = runState,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    private val stopSdk: StopSdkAction by lazy {
        StopSdkAction(
            runState,
            infrastructure.sessionHandler,
            graph.sync.dataSync,
            graph.inApp.inAppPresenter,
            graph.inbox.inboxCountPoller,
            core.runner
        )
    }

    val setSessionDuration: SetSessionDurationAction by lazy {
        SetSessionDurationAction(infrastructure.sessionHandler, core.runner)
    }

    val router: InitRouter by lazy {
        InitRouter(core, this, graph.inApp)
    }
}
