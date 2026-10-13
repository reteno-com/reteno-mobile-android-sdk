package com.reteno.core.fixtures

import com.reteno.core.internal.action.trace.MonotonicClock
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.testDispatchers(): RetenoDispatchers {
    val dispatcher = StandardTestDispatcher(testScheduler)
    return object : RetenoDispatchers {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.virtualClock(): MonotonicClock = MonotonicClock { testScheduler.currentTime * 1_000_000 }

internal class CountingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
    var dispatchCount = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        delegate.dispatch(context, block)
    }
}
