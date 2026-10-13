package com.reteno.core.internal.action.trace

import com.reteno.core.fixtures.FailureReport
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionRunnerTest {

    private class SutFixture(scope: TestScope, enabled: Boolean = true) {
        private val traced = TestActionRunner(scope, enabled)
        val sink = traced.sink
        val reports = traced.reports
        val sut = traced.runner
    }

    @Test
    fun `should return value and log start step and end lines`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("LogEvent") {
            args("type" to "purchase")
            step("save event to local database", detail = { rows: Int -> "rows=$rows" }) {
                delay(4)
                1
            }
            "done"
        }

        then()
        assertEquals("done", result.getOrNull())
        assertEquals(
            listOf(
                TraceFormatter.start(0, "r1", "LogEvent", "type=purchase"),
                TraceFormatter.stepSuccess(0, "r1", "save event to local database", "rows=1", 4),
                TraceFormatter.end(0, "r1", "LogEvent", "✓ SUCCESS", 4)
            ),
            fixture.sink.lines
        )
        assertTrue(fixture.sink.entries.all { it.priority == LogPriority.DEBUG && it.tag == "RetenoAction" })
        assertTrue(fixture.reports.isEmpty())
    }

    @Test
    fun `should join several arguments in start line`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("SetUserAttributes") { args("externalUserId" to "…8f21", "user" to "{email}") }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "SetUserAttributes", "externalUserId=…8f21  user={email}"), fixture.sink.lines.first())
    }

    @Test
    fun `should evaluate lazy argument value when logging`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("SetUserAttributes") { args("user" to { "{firstName}" }) }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "SetUserAttributes", "user={firstName}"), fixture.sink.lines.first())
    }

    @Test
    fun `should print null argument value`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("SetUserAttributes") { args("user" to null) }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "SetUserAttributes", "user=null"), fixture.sink.lines.first())
    }

    @Test
    fun `should print start line without arguments before first step when args are not given`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushUser") { step("load users") { Unit } }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "PushUser", ""), fixture.sink.lines.first())
        assertEquals(3, fixture.sink.lines.size)
    }

    @Test
    fun `should print arguments given after first step as note`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushUser") {
            step("load users") { Unit }
            args("count" to 2)
        }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "PushUser", ""), fixture.sink.lines[0])
        assertEquals(TraceFormatter.note(0, "r1", "count=2"), fixture.sink.lines[2])
    }

    @Test
    fun `should print start line once when args are given twice`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushUser") {
            args("first" to 1)
            args("second" to 2)
        }

        then()
        assertEquals(
            listOf(
                TraceFormatter.start(0, "r1", "PushUser", "first=1"),
                TraceFormatter.note(0, "r1", "second=2"),
                TraceFormatter.end(0, "r1", "PushUser", "✓ SUCCESS", 0)
            ),
            fixture.sink.lines
        )
    }

    @Test
    fun `should print parent start line before child start line`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("Sync") { fixture.sut("PushDevice") { args("count" to 1) } }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "Sync", ""), fixture.sink.lines[0])
        assertEquals(TraceFormatter.start(1, "r1.1", "PushDevice", "count=1"), fixture.sink.lines[1])
    }

    @Test
    fun `should print start line before failure end line when action fails immediately`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut<Unit>("Broken") { throw IllegalStateException("boom") }

        then()
        assertEquals(TraceFormatter.start(0, "r1", "Broken", ""), fixture.sink.lines[0])
        assertEquals(TraceFormatter.end(0, "r1", "Broken", "✗ FAILURE IllegalStateException: boom", 0), fixture.sink.lines[1])
    }

    @Test
    fun `should log step without detail by default`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("LogEvent") { step("notify in-app message triggers") { Unit } }

        then()
        assertEquals(TraceFormatter.stepSuccess(0, "r1", "notify in-app message triggers", "", 0), fixture.sink.lines[1])
    }

    @Test
    fun `should log note`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushUser") { note { "nothing to send" } }

        then()
        assertEquals(TraceFormatter.note(0, "r1", "nothing to send"), fixture.sink.lines[1])
    }

    private enum class TestOutcome(override val traceLabel: String, override val isSuccessful: Boolean) : TracedOutcome {
        PASSED("CONTINUE", true),
        STOPPED("ABORT", false)
    }

    @Test
    fun `should use successful traced outcome as end line label`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushDevice") { TestOutcome.PASSED }

        then()
        val end = fixture.sink.entries.last()
        assertEquals(TraceFormatter.end(0, "r1", "PushDevice", "✓ CONTINUE", 0), end.message)
        assertEquals(LogPriority.DEBUG, end.priority)
    }

    @Test
    fun `should mark unsuccessful traced outcome as warning in end line`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("PushDevice") { TestOutcome.STOPPED }

        then()
        assertEquals(TestOutcome.STOPPED, result.getOrNull())
        val end = fixture.sink.entries.last()
        assertEquals(TraceFormatter.end(0, "r1", "PushDevice", "✗ ABORT", 0), end.message)
        assertEquals(LogPriority.WARN, end.priority)
        assertTrue(fixture.reports.isEmpty())
    }

    @Test
    fun `should show successful traced step value as detail`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushDevice") { step("send device to server") { TestOutcome.PASSED } }

        then()
        val line = fixture.sink.entries[1]
        assertEquals(TraceFormatter.stepSuccess(0, "r1", "send device to server", "CONTINUE", 0), line.message)
        assertEquals(LogPriority.DEBUG, line.priority)
    }

    @Test
    fun `should mark unsuccessful traced step value as warning`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushDevice") { step("send device to server") { TestOutcome.STOPPED } }

        then()
        val line = fixture.sink.entries[1]
        assertEquals(TraceFormatter.stepFailure(0, "r1", "send device to server", "ABORT", 0), line.message)
        assertEquals(LogPriority.WARN, line.priority)
    }

    @Test
    fun `should prefer explicit detail over traced step label`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("PushDevice") { step("send device to server", detail = { _: TestOutcome -> "custom" }) { TestOutcome.STOPPED } }

        then()
        assertEquals(TraceFormatter.stepFailure(0, "r1", "send device to server", "custom", 0), fixture.sink.lines[1])
    }

    @Test
    fun `should return failure and report failed step when step throws`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("boom")

        whenn()
        val result = fixture.sut("Sync") {
            step("send device to server") {
                delay(230)
                throw error
            }
        }

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(listOf(FailureReport("Sync", "send device to server", error)), fixture.reports)
        val entries = fixture.sink.entries
        assertEquals(TraceFormatter.stepFailure(0, "r1", "send device to server", "IOException: boom", 230), entries[1].message)
        assertEquals(TraceFormatter.end(0, "r1", "Sync", "✗ FAILURE IOException: boom", 230), entries[2].message)
        assertTrue(entries[3].message.startsWith("java.io.IOException: boom"))
        assertEquals(listOf(LogPriority.DEBUG, LogPriority.ERROR, LogPriority.ERROR, LogPriority.ERROR), entries.map { it.priority })
    }

    @Test
    fun `should report no step when action throws outside of steps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IllegalStateException("outside")

        whenn()
        fixture.sut<Unit>("Sync") { throw error }

        then()
        assertEquals(listOf(FailureReport("Sync", null, error)), fixture.reports)
    }

    @Test
    fun `should not attribute failure to step whose error was handled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val handled = IOException("handled")
        val final = IllegalStateException("final")

        whenn()
        fixture.sut<Unit>("Sync") {
            runCatching { step("optional step") { throw handled } }
            throw final
        }

        then()
        assertNull(fixture.reports.single().step)
    }

    @Test
    fun `should rethrow cancellation and log cancelled end line`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val started = CompletableDeferred<Unit>()
        var reachedAfterCancel = false

        whenn()
        val job = launch {
            fixture.sut("LongRunning") {
                step("wait forever") {
                    started.complete(Unit)
                    CompletableDeferred<Unit>().await()
                }
            }
            reachedAfterCancel = true
        }
        runCurrent()
        job.cancel(CancellationException("stop"))
        runCurrent()

        then()
        assertTrue(job.isCancelled)
        assertFalse(reachedAfterCancel)
        assertEquals(TraceFormatter.end(0, "r1", "LongRunning", "⊘ CANCELLED", 0), fixture.sink.lines.last())
        assertEquals(2, fixture.sink.lines.size)
        assertTrue(fixture.reports.isEmpty())
    }

    @Test
    fun `should nest child actions under parent id and depth`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("Sync") {
            fixture.sut("PushDevice") {
                fixture.sut("Inner") { Unit }
            }
            fixture.sut("PushUser") { Unit }
        }

        then()
        assertEquals(
            listOf(
                TraceFormatter.start(0, "r1", "Sync", ""),
                TraceFormatter.start(1, "r1.1", "PushDevice", ""),
                TraceFormatter.start(2, "r1.1.1", "Inner", ""),
                TraceFormatter.end(2, "r1.1.1", "Inner", "✓ SUCCESS", 0),
                TraceFormatter.end(1, "r1.1", "PushDevice", "✓ SUCCESS", 0),
                TraceFormatter.start(1, "r1.2", "PushUser", ""),
                TraceFormatter.end(1, "r1.2", "PushUser", "✓ SUCCESS", 0),
                TraceFormatter.end(0, "r1", "Sync", "✓ SUCCESS", 0)
            ),
            fixture.sink.lines
        )
    }

    @Test
    fun `should start new root for separate actions`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("First") { Unit }
        fixture.sut("Second") { Unit }

        then()
        assertEquals(TraceFormatter.start(0, "r2", "Second", ""), fixture.sink.lines[2])
    }

    @Test
    fun `should keep parent successful when child fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("child")

        whenn()
        val result = fixture.sut("Sync") {
            fixture.sut<Unit>("PushDevice") { throw error }.isFailure
        }

        then()
        assertEquals(true, result.getOrNull())
        assertEquals(listOf(FailureReport("PushDevice", null, error)), fixture.reports)
    }

    @Test
    fun `should not log nor build messages when logging disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this, enabled = false)
        var messagesBuilt = 0

        whenn()
        fixture.sut("Quiet") {
            args("args" to { messagesBuilt++; "args" })
            step("step", detail = { _: Unit -> messagesBuilt++; "detail" }) { Unit }
            note { messagesBuilt++; "note" }
        }

        then()
        assertTrue(fixture.sink.entries.isEmpty())
        assertEquals(0, messagesBuilt)
    }

    @Test
    fun `should still report failure when logging disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this, enabled = false)
        val error = IOException("quiet failure")

        whenn()
        fixture.sut<Unit>("Quiet") { step("step") { throw error } }

        then()
        assertTrue(fixture.sink.entries.isEmpty())
        assertEquals(listOf(FailureReport("Quiet", "step", error)), fixture.reports)
    }

    @Test
    fun `should not report failure when reporting disabled for action`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("insert failed")

        whenn()
        val result = fixture.sut<Unit>("LogRetenoEvent", reportFailure = false) { step("save log event to local database") { throw error } }

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.reports.isEmpty())
        assertEquals(LogPriority.ERROR, fixture.sink.entries.last().priority)
    }

    @Test
    fun `should generate six character root id and use system clock by default`() = runUnitTest {
        given()
        val sink = FakeLogSink()
        val sut = ActionRunner(sink = sink, failureReporter = { _, _, _ -> })

        whenn()
        sut("Default") { Unit }

        then()
        assertTrue(Regex("""┌─ \[[0-9a-f]{6}] Default""").matches(sink.lines.first()))
        assertTrue(Regex("""└─ \[[0-9a-f]{6}] Default ✓ SUCCESS  \d+ ms""").matches(sink.lines.last()))
    }
}
