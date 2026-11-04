package com.reteno.core.internal.lifecycle

import com.reteno.core.internal.action.event.LogScreenViewEvent
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ScreenTracker(
    private val logScreenView: LogScreenViewEvent,
    private val scope: CoroutineScope
) {

    @Volatile
    private var config = ScreenTrackingConfig(enable = false)

    fun configure(config: ScreenTrackingConfig) {
        this.config = config
    }

    fun onScreen(name: String, trigger: ScreenTrackingTrigger) {
        val current = config
        if (current.enable && current.trigger == trigger && name !in current.excludeScreens) {
            scope.launch { logScreenView(name) }
        }
    }
}
