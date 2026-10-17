package com.reteno.core.internal.action.init

import com.reteno.core.RetenoConfig

internal interface SdkControl {
    val isStarted: Boolean

    val isInitialized: Boolean

    fun runAfterInit(operation: () -> Unit)

    fun runAfterUserInit(operation: () -> Unit)

    fun boot()

    fun applyUserConfig(config: RetenoConfig)

    fun requestStart()

    fun requestStop()

    fun submitSessionDuration(durationMillis: Long)
}
