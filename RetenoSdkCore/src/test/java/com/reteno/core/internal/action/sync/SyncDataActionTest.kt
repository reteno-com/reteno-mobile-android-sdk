package com.reteno.core.internal.action.sync

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncDataActionTest {

    private class SutFixture(scope: TestScope) {
        val calls = mutableListOf<String>()
        val outcomes = mutableMapOf<String, Result<StepOutcome>>()
        val traced = TestActionRunner(scope)

        private fun step(name: String) = SyncStep {
            calls += name
            outcomes[name] ?: Result.success(StepOutcome.CONTINUE)
        }

        val sut = SyncDataAction(
            contactSteps = listOf(step("device"), step("user")),
            dataSteps = listOf(step("interactions"), step("events"), step("logs")),
            runner = traced.runner
        )
    }

    @Test
    fun `should run contact steps then data steps in order for full sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(SyncScope.ALL).getOrThrow()

        then()
        assertEquals(SyncResult.COMPLETED, result)
        assertEquals(listOf("device", "user", "interactions", "events", "logs"), fixture.calls)
    }

    @Test
    fun `should run only contact steps for contact sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(SyncScope.CONTACT).getOrThrow()

        then()
        assertEquals(SyncResult.COMPLETED, result)
        assertEquals(listOf("device", "user"), fixture.calls)
    }

    @Test
    fun `should stop at first aborted step`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcomes["interactions"] = Result.success(StepOutcome.ABORT)

        whenn()
        val result = fixture.sut(SyncScope.ALL).getOrThrow()

        then()
        assertEquals(SyncResult.ABORTED, result)
        assertEquals(listOf("device", "user", "interactions"), fixture.calls)
        assertTrue(fixture.traced.sink.lines.any { it.contains("stopped after step 3 of 5") })
    }

    @Test
    fun `should continue with next data step but report incomplete sync when a data step crashes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcomes["events"] = Result.failure(IllegalStateException("broken row"))

        whenn()
        val result = fixture.sut(SyncScope.ALL).getOrThrow()

        then()
        assertEquals(SyncResult.INCOMPLETE, result)
        assertEquals(listOf("device", "user", "interactions", "events", "logs"), fixture.calls)
        assertTrue(fixture.traced.sink.lines.any { it.contains("step 4 of 5 failed") })
    }

    @Test
    fun `should stop sync when a contact step crashes so user is never sent before device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcomes["device"] = Result.failure(IllegalStateException("broken device row"))

        whenn()
        val result = fixture.sut(SyncScope.ALL).getOrThrow()

        then()
        assertEquals(SyncResult.ABORTED, result)
        assertEquals(listOf("device"), fixture.calls)
    }

    @Test
    fun `should run each step only while holding the shared step lock`() = runUnitTest {
        given()
        val lock = Mutex(locked = true)
        val calls = mutableListOf<String>()
        val traced = TestActionRunner(this)
        val sut = SyncDataAction(listOf(SyncStep { calls += "device"; Result.success(StepOutcome.CONTINUE) }), emptyList(), traced.runner, lock)

        whenn()
        val run = async { sut(SyncScope.ALL) }
        runCurrent()
        val callsWhileLocked = calls.toList()
        lock.unlock()
        run.await()

        then()
        assertTrue(callsWhileLocked.isEmpty())
        assertEquals(listOf("device"), calls)
    }

    @Test
    fun `should trace scope and mark aborted sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcomes["device"] = Result.success(StepOutcome.ABORT)

        whenn()
        fixture.sut(SyncScope.CONTACT)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("Sync  scope=CONTACT"))
        assertTrue(lines.last().contains("Sync ✗ ABORTED"))
    }

    @Test
    fun `should fail and report once when a step throws instead of returning failure`() = runUnitTest {
        given()
        val traced = TestActionRunner(this)
        val sut = SyncDataAction(listOf(SyncStep { throw IllegalStateException("boom") }), emptyList(), traced.runner)

        whenn()
        val result = sut(SyncScope.ALL)

        then()
        assertTrue(result.isFailure)
        assertEquals(1, traced.reports.size)
    }
}
