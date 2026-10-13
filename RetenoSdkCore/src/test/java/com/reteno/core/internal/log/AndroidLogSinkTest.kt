package com.reteno.core.internal.log

import android.util.Log
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AndroidLogSinkTest {

    private class SutFixture(enabled: Boolean = true) {
        val printed = mutableListOf<Triple<Int, String, String>>()
        val sut = AndroidLogSink(
            enabled = { enabled },
            printer = { priority, tag, message -> printed += Triple(priority, tag, message) }
        )
    }

    @Test
    fun `should print message with android priority and tag`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.write(LogPriority.WARN, "RetenoAction", "hello")

        then()
        assertEquals(listOf(Triple(Log.WARN, "RetenoAction", "hello")), fixture.printed)
    }

    @Test
    fun `should split message longer than logcat limit into chunks`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = "a".repeat(AndroidLogSink.MAX_CHUNK) + "b".repeat(10)

        whenn()
        fixture.sut.write(LogPriority.ERROR, "T", message)

        then()
        assertEquals(
            listOf(
                Triple(Log.ERROR, "T", "a".repeat(AndroidLogSink.MAX_CHUNK)),
                Triple(Log.ERROR, "T", "b".repeat(10))
            ),
            fixture.printed
        )
    }

    @Test
    fun `should report enabled when predicate returns true`() = runUnitTest {
        given()
        val fixture = SutFixture(enabled = true)

        whenn()
        val enabled = fixture.sut.isEnabled

        then()
        assertTrue(enabled)
    }

    @Test
    fun `should report disabled when predicate returns false`() = runUnitTest {
        given()
        val fixture = SutFixture(enabled = false)

        whenn()
        val enabled = fixture.sut.isEnabled

        then()
        assertFalse(enabled)
    }

    @Test
    fun `should enable logging for debug build`() = runUnitTest {
        given()
        val debugBuild = true

        whenn()
        val enabled = isLoggingEnabled(debugBuild = debugBuild, debugView = false)

        then()
        assertTrue(enabled)
    }

    @Test
    fun `should enable logging for debug view`() = runUnitTest {
        given()
        val debugView = true

        whenn()
        val enabled = isLoggingEnabled(debugBuild = false, debugView = debugView)

        then()
        assertTrue(enabled)
    }

    @Test
    fun `should disable logging for release build without debug view`() = runUnitTest {
        given()
        val debugBuild = false

        whenn()
        val enabled = isLoggingEnabled(debugBuild = debugBuild, debugView = false)

        then()
        assertFalse(enabled)
    }

    @Test
    fun `should map every priority to android log priority`() = runUnitTest {
        given()
        val expected = mapOf(
            LogPriority.DEBUG to Log.DEBUG,
            LogPriority.INFO to Log.INFO,
            LogPriority.WARN to Log.WARN,
            LogPriority.ERROR to Log.ERROR
        )

        whenn()
        val actual = LogPriority.values().associateWith { it.androidPriority }

        then()
        assertEquals(expected, actual)
    }
}
