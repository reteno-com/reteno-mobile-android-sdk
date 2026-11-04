package com.reteno.core.internal.action.lifecycle

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.lifecycle.ScreenTracker
import com.reteno.core.internal.platform.HostUi
import com.reteno.core.lifecycle.ScreenTrackingConfig
import java.util.concurrent.atomic.AtomicBoolean

internal class EnableAutoScreenTrackingAction(
    private val tracker: ScreenTracker,
    private val hostUi: HostUi,
    private val runner: ActionRunner
) {

    private val watching = AtomicBoolean(false)

    suspend operator fun invoke(config: ScreenTrackingConfig): Result<Unit> = runner("EnableAutoScreenTracking") {
        args("enable" to config.enable, "trigger" to config.trigger, "excluded" to config.excludeScreens.size)
        configure(config)
    }

    private suspend fun ActionTrace.configure(config: ScreenTrackingConfig) {
        if (watching.compareAndSet(false, true)) {
            step("start watching fragment screens") {
                hostUi.watchFragmentScreens(tracker::onScreen)
            }
        } else {
            note { "already watching fragment screens" }
        }
        step("apply screen tracking options") {
            tracker.configure(config)
        }
    }
}
