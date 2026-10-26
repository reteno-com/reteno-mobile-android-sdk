package com.reteno.core.internal.action.trace

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class StepOrNullTest {

    @Test
    fun `should return step value when step succeeds`() = runUnitTest {
        given()
        val traced = TestActionRunner(this)

        whenn()
        val result = traced.runner("Read") {
            stepOrNull("read value") {
                42
            }
        }

        then()
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `should return null and keep action successful when step fails`() = runUnitTest {
        given()
        val traced = TestActionRunner(this)

        whenn()
        val result = traced.runner("Read") {
            stepOrNull<Int>("read value") {
                throw IOException("broken")
            }
        }

        then()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(traced.sink.lines.any { it.contains("✗ read value") })
        assertTrue(traced.reports.isEmpty())
    }

    @Test
    fun `should rethrow cancellation instead of returning null`() = runUnitTest {
        given()
        val traced = TestActionRunner(this)
        var cancelled = false

        whenn()
        try {
            traced.runner("Read") {
                stepOrNull<Int>("read value") {
                    throw CancellationException("stopped")
                }
            }
        } catch (expected: CancellationException) {
            cancelled = true
        }

        then()
        assertTrue(cancelled)
        assertTrue(traced.sink.lines.last().contains("⊘ CANCELLED"))
    }
}
