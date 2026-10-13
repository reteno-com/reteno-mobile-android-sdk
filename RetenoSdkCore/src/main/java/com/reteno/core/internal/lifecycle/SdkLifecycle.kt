package com.reteno.core.internal.lifecycle

internal interface SdkLifecycle {
    val isStarted: Boolean

    fun stop()

    fun start()
}
