package com.reteno.core.util

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.RemoteLog
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LoggerTest {

    private class SutFixture(loggingEnabled: Boolean = true, val remoteLog: FakeRemoteLog? = FakeRemoteLog()) {
        val sink = FakeLogSink(isEnabled = loggingEnabled)

        inline fun <T> withLogger(block: () -> T): T {
            Logger.install(sink, remoteLog as RemoteLog?)
            try {
                return block()
            } finally {
                Logger.install(AndroidLogSink(), null)
            }
        }
    }

    @Test
    fun `should write messages to sink with their priority and tag`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withLogger {
            Logger.v("Tag", "verbose(): ", "a")
            Logger.d("Tag", "debug(): ", "b", 1)
            Logger.i("Tag", "info(): ")
            Logger.w("Tag", "warn(): ", null)
        }

        then()
        val expected = listOf(
            Triple(LogPriority.VERBOSE, "Tag", "verbose(): a"),
            Triple(LogPriority.DEBUG, "Tag", "debug(): b1"),
            Triple(LogPriority.INFO, "Tag", "info(): "),
            Triple(LogPriority.WARN, "Tag", "warn(): null")
        )
        assertEquals(expected, fixture.sink.entries.map { Triple(it.priority, it.tag, it.message) })
    }

    @Test
    fun `should not build messages when logging is off`() = runUnitTest {
        given()
        val fixture = SutFixture(loggingEnabled = false)
        var built = 0
        val argument = object {
            override fun toString(): String {
                built++
                return "argument"
            }
        }

        whenn()
        fixture.withLogger {
            Logger.i("Tag", "info(): ", argument)
            Logger.e("Tag", "error(): ", IllegalStateException("boom"), argument)
        }

        then()
        assertEquals(0, built)
        assertTrue(fixture.sink.entries.isEmpty())
    }

    @Test
    fun `should write error with stack trace to sink`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withLogger { Logger.e("Tag", "save(): ", IllegalStateException("disk full")) }

        then()
        val entry = fixture.sink.entries.single()
        assertEquals(LogPriority.ERROR, entry.priority)
        assertTrue(entry.message.startsWith("save(): \njava.lang.IllegalStateException: disk full"))
    }

    @Test
    fun `should write error arguments to sink but record only method name remotely`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException("disk full")

        whenn()
        fixture.withLogger { Logger.e("Tag", "save(): ", error, "user = ", "john@example.com") }

        then()
        assertTrue(fixture.sink.lines.single().startsWith("save(): user = john@example.com\n"))
        assertEquals(listOf("save(): " to error), fixture.remoteLog!!.errors)
    }

    @Test
    fun `should record error remotely`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException("disk full")

        whenn()
        fixture.withLogger { Logger.e("Tag", "save(): ", error) }

        then()
        assertEquals(listOf("save(): " to error), fixture.remoteLog!!.errors)
    }

    @Test
    fun `should record captured messages with their level`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withLogger {
            Logger.captureMessage("default level")
            Logger.captureMessage("warning", LogLevel.WARNING)
        }

        then()
        val expected = listOf("default level" to LogLevel.INFO, "warning" to LogLevel.WARNING)
        assertEquals(expected, fixture.remoteLog!!.entries)
    }

    @Test
    fun `should record captured event as given`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val event = stubRetenoLogEvent(logLevel = LogLevel.FATAL, errorMessage = "crash")

        whenn()
        fixture.withLogger { Logger.captureEvent(event) }

        then()
        assertEquals(listOf(event), fixture.remoteLog!!.events)
    }

    @Test
    fun `should record what was logged before sdk installs remote log once it does`() = runUnitTest {
        given()
        val fixture = SutFixture(remoteLog = null)
        val installed = FakeRemoteLog()
        val error = IllegalStateException("boom")
        val event = stubRetenoLogEvent()
        fixture.withLogger {
            Logger.captureMessage("before init")
            Logger.captureEvent(event)
            Logger.e("Tag", "save(): ", error)
            Logger.e("Tag", "load(): ", error, "argument")

            whenn()
            Logger.install(fixture.sink, installed)
        }

        then()
        assertEquals(listOf("before init" to LogLevel.INFO), installed.entries)
        assertEquals(listOf(event), installed.events)
        assertEquals(listOf("save(): " to error, "load(): " to error), installed.errors)
    }

    @Test
    fun `should keep only the latest messages logged before sdk installs remote log`() = runUnitTest {
        given()
        val fixture = SutFixture(remoteLog = null)
        val installed = FakeRemoteLog()
        fixture.withLogger {
            repeat(MAX_BEFORE_INSTALL + 2) { Logger.captureMessage("message $it") }

            whenn()
            Logger.install(fixture.sink, installed)
        }

        then()
        assertEquals(MAX_BEFORE_INSTALL, installed.entries.size)
        assertEquals("message 2", installed.entries.first().first)
    }

    @Test
    fun `should forget messages logged before logging is reset`() = runUnitTest {
        given()
        val fixture = SutFixture(remoteLog = null)
        val installed = FakeRemoteLog()
        fixture.withLogger {
            Logger.captureMessage("before reset")
            Logger.install(fixture.sink, null)

            whenn()
            Logger.install(fixture.sink, installed)
        }

        then()
        assertTrue(installed.entries.isEmpty())
    }

    private companion object {
        const val MAX_BEFORE_INSTALL = 50
    }
}
