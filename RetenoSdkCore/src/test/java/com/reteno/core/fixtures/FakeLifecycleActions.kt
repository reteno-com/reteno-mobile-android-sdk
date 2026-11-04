package com.reteno.core.fixtures

import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.action.lifecycle.LifecycleActions
import com.reteno.core.lifecycle.ScreenTrackingConfig

internal class FakeLifecycleActions(private val log: FakeCallLog = FakeCallLog()) : LifecycleActions {
    val screenTrackingConfigs = mutableListOf<ScreenTrackingConfig>()

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        log.record("submitLifecycleEventConfig")
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        log.record("submitAutoScreenTracking")
        screenTrackingConfigs += config
    }

    override fun submitPushPermissionStatus() {
        log.record("submitPushPermissionStatus")
    }
}
