package com.reteno.core.internal.lifecycle

import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.util.Util.toTypeMap

internal class LifecycleEventFilter(options: LifecycleTrackingOptions) {

    @Volatile
    private var enabled: Map<LifecycleEventType, Boolean> = options.toTypeMap()

    fun update(options: LifecycleTrackingOptions) {
        enabled = options.toTypeMap()
    }

    fun isEnabled(type: LifecycleEventType): Boolean = enabled[type] == true
}
