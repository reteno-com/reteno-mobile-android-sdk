package com.reteno.core.fixtures

import com.reteno.core.internal.action.trace.MonotonicClock
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestDispatchers(scope: TestScope) : RetenoDispatchers {
    private val base = StandardTestDispatcher(scope.testScheduler)
    val markedIo = MarkingDispatcher(base)
    val markedMain = MarkingDispatcher(base)

    override val main: CoroutineDispatcher = markedMain
    override val io: CoroutineDispatcher = markedIo
    override val default: CoroutineDispatcher = base
}

internal fun TestScope.testDispatchers(): TestDispatchers = TestDispatchers(this)

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

internal class MarkingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
    private val running = ThreadLocal<Boolean>()

    val isRunningHere: Boolean
        get() = running.get() == true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, Runnable {
            running.set(true)
            try {
                block.run()
            } finally {
                running.set(false)
            }
        })
    }
}
