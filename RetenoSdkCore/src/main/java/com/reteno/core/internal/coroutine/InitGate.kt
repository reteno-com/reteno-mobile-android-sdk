package com.reteno.core.internal.coroutine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class InitState {
    NOT_INITIALIZED,
    FROM_CACHE,
    BY_USER
}

internal class InitGate {

    private val initialized = CompletableDeferred<Unit>()
    private val initializedByUser = CompletableDeferred<Unit>()
    private val lock = Any()
    private val held = mutableListOf<HeldOperation>()
    private var releasedNotRun = 0

    val state: InitState
        get() = when {
            initializedByUser.isCompleted -> InitState.BY_USER
            initialized.isCompleted -> InitState.FROM_CACHE
            else -> InitState.NOT_INITIALIZED
        }

    val isInitialized: Boolean
        get() = initialized.isCompleted

    val isInitializedByUser: Boolean
        get() = initializedByUser.isCompleted

    fun markInitialized(byUser: Boolean) {
        val released = synchronized(lock) {
            initialized.complete(Unit)
            if (byUser) initializedByUser.complete(Unit)
            val ready = held.filter { it.isReady() }
            held.removeAll(ready)
            ready
        }
        released.forEach(::launch)
    }

    suspend fun awaitInit() {
        initialized.await()
    }

    suspend fun awaitUserInit() {
        initializedByUser.await()
    }

    fun runAfterInit(scope: CoroutineScope, operation: () -> Unit) {
        submit(HeldOperation(scope, operation, needsUser = false))
    }

    fun runAfterUserInit(scope: CoroutineScope, operation: () -> Unit) {
        submit(HeldOperation(scope, operation, needsUser = true))
    }

    private fun submit(operation: HeldOperation) {
        val runNow = synchronized(lock) {
            if (!operation.isReady()) {
                held += operation
                return
            }
            releasedNotRun == 0
        }
        if (runNow) {
            operation.block()
        } else {
            launch(operation)
        }
    }

    private fun launch(operation: HeldOperation) {
        if (!operation.scope.isActive) return
        synchronized(lock) { releasedNotRun++ }
        operation.scope.launch { operation.block() }.invokeOnCompletion {
            synchronized(lock) { releasedNotRun-- }
        }
    }

    private fun HeldOperation.isReady(): Boolean =
        if (needsUser) initializedByUser.isCompleted else initialized.isCompleted

    private class HeldOperation(
        val scope: CoroutineScope,
        val block: () -> Unit,
        val needsUser: Boolean
    )
}
