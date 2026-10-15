package com.reteno.core.internal.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal interface RetenoDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

internal fun retenoDispatchers(main: CoroutineDispatcher, io: CoroutineDispatcher): RetenoDispatchers =
    object : RetenoDispatchers {
        override val main: CoroutineDispatcher = main
        override val io: CoroutineDispatcher = io
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
