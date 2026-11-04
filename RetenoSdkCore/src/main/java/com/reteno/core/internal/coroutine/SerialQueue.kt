package com.reteno.core.internal.coroutine

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class SerialQueue(
    scope: CoroutineScope,
    private val name: String,
    private val sink: RetenoLogSink
) {

    private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            while (true) {
                run(tasks.receive())
            }
        }
    }

    fun enqueue(task: suspend () -> Unit) {
        tasks.trySend(task)
    }

    private suspend fun run(task: suspend () -> Unit) {
        try {
            task()
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            sink.log(LogPriority.ERROR, TAG) { "$name task failed: ${Redact.stackTrace(error)}" }
        }
    }

    private companion object {
        const val TAG = "Reteno"
    }
}
