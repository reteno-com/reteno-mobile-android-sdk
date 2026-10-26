package com.reteno.core.internal.action.init

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.lifecycle.RetenoSessionHandler

internal class SetSessionDurationAction(
    private val sessionHandler: RetenoSessionHandler,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(durationMillis: Long): Result<Unit> = runner("SetSessionDuration") {
        args("durationMillis" to durationMillis)
        step("apply session duration to session tracking") {
            sessionHandler.setSessionResetDuration(durationMillis)
        }
    }
}
