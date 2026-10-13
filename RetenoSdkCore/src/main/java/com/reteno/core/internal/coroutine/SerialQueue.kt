package com.reteno.core.internal.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class SerialQueue(
    scope: CoroutineScope,
    private val onFailure: (Throwable) -> Unit
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
            onFailure(error)
        }
    }
}
