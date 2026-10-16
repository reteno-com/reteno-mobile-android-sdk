package com.reteno.core.internal.coroutine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal enum class InitState {
    NOT_INITIALIZED,
    FROM_CACHE,
    BY_USER
}

internal class InitGate {

    private val initialized = CompletableDeferred<Unit>()
    private val initializedByUser = CompletableDeferred<Unit>()

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
        initialized.complete(Unit)
        if (byUser) initializedByUser.complete(Unit)
    }

    suspend fun awaitInit() {
        initialized.await()
    }

    fun runAfterInit(scope: CoroutineScope, operation: () -> Unit) {
        if (isInitialized) {
            operation()
            return
        }
        scope.launch {
            awaitInit()
            operation()
        }
    }

    suspend fun awaitUserInit() {
        initializedByUser.await()
    }
}
