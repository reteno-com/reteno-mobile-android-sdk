package com.reteno.core.di.route

import com.reteno.core.RetenoConfig
import com.reteno.core.di.ActionCore
import com.reteno.core.di.graph.InAppGraph
import com.reteno.core.di.graph.InitGraph
import com.reteno.core.internal.action.init.SdkControl
import kotlinx.coroutines.launch

internal class InitRouter(
    private val core: ActionCore,
    private val init: InitGraph,
    private val inApp: InAppGraph
) : SdkControl {

    override val isStarted: Boolean
        get() = init.runState.isStarted

    override val isInitialized: Boolean
        get() = core.initGate.isInitialized

    override fun runAfterInit(operation: () -> Unit) {
        core.initGate.runAfterInit(core.mainScope, operation)
    }

    override fun boot() {
        core.hostUi.trackActivities()
        inApp.inAppTriggers.listen()
        init.initQueue.enqueue { init.bootSdk() }
    }

    override fun applyUserConfig(config: RetenoConfig) {
        core.configuration.useUserConfig(config)
        init.initQueue.enqueue { init.applyConfig(config, byUser = true) }
    }

    override fun requestStart() {
        init.sdk.start()
    }

    override fun requestStop() {
        init.sdk.stop()
    }

    override fun submitSessionDuration(durationMillis: Long) {
        core.scope.launch { init.setSessionDuration(durationMillis) }
    }
}
