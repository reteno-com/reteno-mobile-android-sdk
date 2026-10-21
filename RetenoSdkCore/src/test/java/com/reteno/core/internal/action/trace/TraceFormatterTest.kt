package com.reteno.core.internal.action.trace

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TraceFormatterTest {

    @Test
    fun `should format root start line with arguments`() = runUnitTest {
        given()
        val args = "type=purchase"

        whenn()
        val line = TraceFormatter.start(depth = 0, id = "a1f3c2", name = "LogEvent", args = args)

        then()
        assertEquals("┌─ [a1f3c2] LogEvent  type=purchase", line)
    }

    @Test
    fun `should format start line without arguments`() = runUnitTest {
        given()
        val args = ""

        whenn()
        val line = TraceFormatter.start(depth = 0, id = "a1f3c2", name = "LogEvent", args = args)

        then()
        assertEquals("┌─ [a1f3c2] LogEvent", line)
    }

    @Test
    fun `should indent nested start line by depth`() = runUnitTest {
        given()
        val depth = 2

        whenn()
        val line = TraceFormatter.start(depth = depth, id = "a1.1.1", name = "PushDevice", args = "")

        then()
        assertEquals("│  │  ┌─ [a1.1.1] PushDevice", line)
    }

    @Test
    fun `should format successful step in aligned columns`() = runUnitTest {
        given()
        val detail = "rows=1"

        whenn()
        val line = TraceFormatter.stepSuccess(depth = 0, id = "a1", name = "insert event", detail = detail, millis = 4)

        then()
        assertEquals("│  [a1] ✓ insert event${" ".repeat(21)}rows=1${" ".repeat(12)}4 ms", line)
    }

    @Test
    fun `should format failed step with error in detail column`() = runUnitTest {
        given()
        val error = "IOException"

        whenn()
        val line = TraceFormatter.stepFailure(
            depth = 1,
            id = "a1.2",
            name = "send device to server",
            error = error,
            millis = 230
        )

        then()
        assertEquals("│  │  [a1.2] ✗ send device to server${" ".repeat(12)}IOException${" ".repeat(5)}230 ms", line)
    }

    @Test
    fun `should format note`() = runUnitTest {
        given()
        val message = "nothing to send"

        whenn()
        val line = TraceFormatter.note(depth = 0, id = "a1", message = message)

        then()
        assertEquals("│  [a1] → nothing to send", line)
    }

    @Test
    fun `should format end line with result and total duration`() = runUnitTest {
        given()
        val result = "✓ SUCCESS"

        whenn()
        val line = TraceFormatter.end(depth = 0, id = "a1", name = "LogEvent", result = result, millis = 9)

        then()
        assertEquals("└─ [a1] LogEvent ✓ SUCCESS  9 ms", line)
    }

    @Test
    fun `should format collapsed action with arguments result and total duration on one line`() = runUnitTest {
        given()
        val args = "scope=ALL"

        whenn()
        val line = TraceFormatter.collapsed(
            depth = 0,
            id = "a1",
            name = "Sync",
            args = args,
            result = "✓ COMPLETED  sent=PushEvents",
            millis = 412
        )

        then()
        assertEquals("── [a1] Sync  scope=ALL ✓ COMPLETED  sent=PushEvents  412 ms", line)
    }

    @Test
    fun `should format collapsed action without arguments indented by depth`() = runUnitTest {
        given()
        val depth = 1

        whenn()
        val line = TraceFormatter.collapsed(
            depth = depth,
            id = "a1.1",
            name = "Sync",
            args = "",
            result = "✓ COMPLETED",
            millis = 3
        )

        then()
        assertEquals("│  ── [a1.1] Sync ✓ COMPLETED  3 ms", line)
    }

    @Test
    fun `should truncate lines longer than limit`() = runUnitTest {
        given()
        val args = "x".repeat(TraceFormatter.MAX_LINE)

        whenn()
        val line = TraceFormatter.start(depth = 0, id = "a1", name = "Long", args = args)

        then()
        assertEquals(TraceFormatter.MAX_LINE, line.length)
        assertEquals('…', line.last())
    }

    @Test
    fun `should keep line exactly at limit untouched`() = runUnitTest {
        given()
        val prefix = "┌─ [a1] Long  "
        val args = "x".repeat(TraceFormatter.MAX_LINE - prefix.length)

        whenn()
        val line = TraceFormatter.start(depth = 0, id = "a1", name = "Long", args = args)

        then()
        assertEquals(prefix + args, line)
    }
}
