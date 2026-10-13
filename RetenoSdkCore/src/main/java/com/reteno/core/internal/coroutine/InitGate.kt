package com.reteno.core.internal.coroutine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal enum class InitState {
    NOT_INITIALIZED,
    FROM_CACHE,
    BY_USER
}

internal class InitGate {

    private val current = MutableStateFlow(InitState.NOT_INITIALIZED)

    val state: InitState
        get() = current.value

    val isInitialized: Boolean
        get() = state >= InitState.FROM_CACHE

    val isInitializedByUser: Boolean
        get() = state == InitState.BY_USER

    fun markInitialized(byUser: Boolean) {
        val reached = if (byUser) InitState.BY_USER else InitState.FROM_CACHE
        current.update { maxOf(it, reached) }
    }

    suspend fun awaitInit() {
        current.first { it >= InitState.FROM_CACHE }
    }

    suspend fun awaitUserInit() {
        current.first { it == InitState.BY_USER }
    }
}
