package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.lifecycle.LifecycleEventFilter

internal class SetLifecycleEventConfigAction(
    private val filter: LifecycleEventFilter,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(options: LifecycleTrackingOptions): Result<Unit> = runner("SetLifecycleEventConfig") {
        args("options" to options)
        step("apply lifecycle tracking options") { filter.update(options) }
    }
}
