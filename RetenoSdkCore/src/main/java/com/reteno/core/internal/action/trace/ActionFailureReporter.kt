package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog

internal fun interface ActionFailureReporter {
    fun report(action: String, step: String?, error: Throwable)
}

internal class RemoteLogFailureReporter(
    private val remoteLog: RemoteLog
) : ActionFailureReporter {

    override fun report(action: String, step: String?, error: Throwable) {
        val location = step?.let { " at step '$it'" }.orEmpty()
        remoteLog.error("Action $action failed$location: ${Redact.summary(error)}")
    }
}
