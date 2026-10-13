package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.Redact

internal fun interface ActionFailureReporter {
    fun report(action: String, step: String?, error: Throwable)
}

internal val NoFailureReporting = ActionFailureReporter { _, _, _ -> }

internal class RemoteLogFailureReporter(
    private val capture: (String) -> Unit
) : ActionFailureReporter {

    override fun report(action: String, step: String?, error: Throwable) {
        val location = step?.let { " at step '$it'" }.orEmpty()
        capture("Action $action failed$location: ${Redact.summary(error)}")
    }
}
