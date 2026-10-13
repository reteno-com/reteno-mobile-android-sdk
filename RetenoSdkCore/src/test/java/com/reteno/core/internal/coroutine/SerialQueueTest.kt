package com.reteno.core.internal.coroutine

import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SerialQueueTest {

    private class SutFixture(scope: TestScope) {
        val sink = FakeLogSink()
        val failures: List<String>
            get() = sink.entries.filter { it.priority == LogPriority.ERROR }.map { it.message }
        val sut = SerialQueue(scope.workScope(), "Test", sink)
    }

    @Test
    fun `should run tasks one after another in enqueue order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val log = mutableListOf<String>()
        val firstGate = CompletableDeferred<Unit>()

        whenn()
        fixture.sut.enqueue { log += "first start"; firstGate.await(); log += "first end" }
        fixture.sut.enqueue { log += "second" }
        runCurrent()
        val whileFirstRuns = log.toList()
        firstGate.complete(Unit)
        runCurrent()

        then()
        assertEquals(listOf("first start"), whileFirstRuns)
        assertEquals(listOf("first start", "first end", "second"), log)
    }

    @Test
    fun `should keep running next tasks after a task fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IllegalStateException("boom")
        var secondRan = false

        whenn()
        fixture.sut.enqueue { throw error }
        fixture.sut.enqueue { secondRan = true }
        runCurrent()

        then()
        assertTrue(secondRan)
        assertEquals(1, fixture.failures.size)
        assertTrue(fixture.failures.single().startsWith("Test task failed: java.lang.IllegalStateException"))
    }

    @Test
    fun `should keep running when a task cancels itself while queue is active`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var secondRan = false

        whenn()
        fixture.sut.enqueue { throw CancellationException("task timed out") }
        fixture.sut.enqueue { secondRan = true }
        runCurrent()

        then()
        assertTrue(secondRan)
        assertTrue(fixture.failures.single().contains("CancellationException"))
    }

    @Test
    fun `should stop when queue scope is cancelled`() = runUnitTest {
        given()
        val scope = workScope()
        val sut = SerialQueue(scope, "Test", FakeLogSink())
        var ran = false

        whenn()
        scope.cancel()
        sut.enqueue { ran = true }
        runCurrent()

        then()
        assertFalse(ran)
    }
}
