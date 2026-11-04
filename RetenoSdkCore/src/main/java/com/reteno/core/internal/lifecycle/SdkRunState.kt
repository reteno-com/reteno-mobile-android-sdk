package com.reteno.core.internal.lifecycle

import java.util.concurrent.atomic.AtomicBoolean

internal class SdkRunState {

    private val started = AtomicBoolean(false)

    val isStarted: Boolean
        get() = started.get()

    fun markStarted(): Boolean = !started.getAndSet(true)

    fun markStopped() {
        started.set(false)
    }
}
