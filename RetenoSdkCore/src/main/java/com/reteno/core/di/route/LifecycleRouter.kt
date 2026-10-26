package com.reteno.core.di.route

import com.reteno.core.di.ActionCore
import com.reteno.core.di.graph.LifecycleGraph
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.action.lifecycle.LifecycleActions
import com.reteno.core.lifecycle.ScreenTrackingConfig
import kotlinx.coroutines.launch

internal class LifecycleRouter(
    private val core: ActionCore,
    private val lifecycle: LifecycleGraph
) : LifecycleActions {

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        core.scope.launch { lifecycle.setLifecycleEventConfig(options) }
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        core.mainScope.launch { lifecycle.enableAutoScreenTracking(config) }
    }

    override fun submitPushPermissionStatus() {
        core.scope.launch(core.dispatchers.io) { lifecycle.updatePushPermissionStatus() }
    }
}
