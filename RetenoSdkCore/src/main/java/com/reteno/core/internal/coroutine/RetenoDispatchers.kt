package com.reteno.core.internal.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal interface RetenoDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

internal object AndroidRetenoDispatchers : RetenoDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

internal fun retenoDispatchers(main: CoroutineDispatcher, io: CoroutineDispatcher): RetenoDispatchers =
    object : RetenoDispatchers {
        override val main: CoroutineDispatcher = main
        override val io: CoroutineDispatcher = io
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
