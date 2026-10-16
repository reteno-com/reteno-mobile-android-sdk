package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.lifecycle.ScreenTrackingConfig

internal interface LifecycleActions {
    fun submitLifecycleEventConfig(options: LifecycleTrackingOptions)

    fun submitAutoScreenTracking(config: ScreenTrackingConfig)

    fun submitPushPermissionStatus()
}
