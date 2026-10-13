package com.reteno.core.internal.action.trace

import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.virtualClock
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

    private data class Report(val action: String, val step: String?, val errorType: Class<*>, val message: String?) {
        constructor(action: String, step: String?, error: Throwable) : this(action, step, error.javaClass, error.message)
    }

    private class SutFixture(scope: TestScope, enabled: Boolean = true) {
        val sink = FakeLogSink(isEnabled = enabled)
        val reports = mutableListOf<Report>()
        private var nextRoot = 0
        val sut = ActionRunner(
            sink = sink,
            failureReporter = { action, step, error -> reports += Report(action, step, error) },
            clock = scope.virtualClock(),
            newRootId = { "r${++nextRoot}" }
        )
    }

    @Test
    fun `should return value and log start step and end lines`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut.run("LogEvent", args = { "type=purchase" }) {
            step("insert event", detail = { rows: Int -> "rows=$rows" }) {
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
                TraceFormatter.stepSuccess(0, "r1", "insert event", "rows=1", 4),
                TraceFormatter.end(0, "r1", "LogEvent", "✓ SUCCESS", 4)
            ),
            fixture.sink.lines
        )
        assertTrue(fixture.sink.entries.all { it.priority == LogPriority.DEBUG && it.tag == "RetenoAction" })
        assertTrue(fixture.reports.isEmpty())
    }

    @Test
    fun `should log step without detail by default`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.run("LogEvent") { step("emit to event bus") { Unit } }

        then()
        assertEquals(TraceFormatter.stepSuccess(0, "r1", "emit to event bus", "", 0), fixture.sink.lines[1])
    }

    @Test
    fun `should log note`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.run("PushUser") { note { "nothing to send" } }

        then()
        assertEquals(TraceFormatter.note(0, "r1", "nothing to send"), fixture.sink.lines[1])
    }

    @Test
    fun `should use outcome label in end line`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.run("PushDevice", outcome = { value: String -> value }) { "CONTINUE" }

        then()
        assertEquals(TraceFormatter.end(0, "r1", "PushDevice", "✓ CONTINUE", 0), fixture.sink.lines.last())
    }

    @Test
    fun `should return failure and report failed step when step throws`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("boom")

        whenn()
        val result = fixture.sut.run("Sync") {
            step("send device to server") {
                delay(230)
                throw error
            }
        }

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(listOf(Report("Sync", "send device to server", error)), fixture.reports)
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
        fixture.sut.run<Unit>("Sync") { throw error }

        then()
        assertEquals(listOf(Report("Sync", null, error)), fixture.reports)
    }

    @Test
    fun `should not attribute failure to step whose error was handled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val handled = IOException("handled")
        val final = IllegalStateException("final")

        whenn()
        fixture.sut.run<Unit>("Sync") {
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
            fixture.sut.run("LongRunning") {
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
        fixture.sut.run("Sync") {
            fixture.sut.run("PushDevice") {
                fixture.sut.run("Inner") { Unit }
            }
            fixture.sut.run("PushUser") { Unit }
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
        fixture.sut.run("First") { Unit }
        fixture.sut.run("Second") { Unit }

        then()
        assertEquals(TraceFormatter.start(0, "r2", "Second", ""), fixture.sink.lines[2])
    }

    @Test
    fun `should keep parent successful when child fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("child")

        whenn()
        val result = fixture.sut.run("Sync") {
            fixture.sut.run<Unit>("PushDevice") { throw error }.isFailure
        }

        then()
        assertEquals(true, result.getOrNull())
        assertEquals(listOf(Report("PushDevice", null, error)), fixture.reports)
    }

    @Test
    fun `should not log nor build messages when logging disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this, enabled = false)
        var messagesBuilt = 0

        whenn()
        fixture.sut.run("Quiet", args = { messagesBuilt++; "args" }) {
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
        fixture.sut.run<Unit>("Quiet") { step("step") { throw error } }

        then()
        assertTrue(fixture.sink.entries.isEmpty())
        assertEquals(listOf(Report("Quiet", "step", error)), fixture.reports)
    }

    @Test
    fun `should generate six character root id and use system clock by default`() = runUnitTest {
        given()
        val sink = FakeLogSink()
        val sut = ActionRunner(sink = sink, failureReporter = { _, _, _ -> })

        whenn()
        sut.run("Default") { Unit }

        then()
        assertTrue(Regex("""┌─ \[[0-9a-f]{6}] Default""").matches(sink.lines.first()))
        assertTrue(Regex("""└─ \[[0-9a-f]{6}] Default ✓ SUCCESS  \d+ ms""").matches(sink.lines.last()))
    }
}
