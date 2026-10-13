package com.reteno.core.internal.coroutine

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private const val TAG = "Reteno"

internal fun retenoScope(dispatchers: RetenoDispatchers, sink: RetenoLogSink): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, error ->
        sink.log(LogPriority.ERROR, TAG) { "Uncaught: ${Redact.stackTrace(error)}" }
    }
    return CoroutineScope(SupervisorJob() + dispatchers.default + handler)
}
