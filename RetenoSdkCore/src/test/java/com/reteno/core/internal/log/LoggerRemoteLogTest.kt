package com.reteno.core.internal.log

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class LoggerRemoteLogTest {

    private class SutFixture {
        val installed = FakeRemoteLog()

        fun <T> withLogger(block: () -> T): T {
            Logger.install(FakeLogSink(), installed)
            try {
                return block()
            } finally {
                Logger.install(AndroidLogSink(), null)
            }
        }
    }

    @Test
    fun `should pass messages to remote log installed in logger`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withLogger {
            LoggerRemoteLog.info("rows removed")
            LoggerRemoteLog.record("slow sync", LogLevel.WARNING)
        }

        then()
        val expected = listOf("rows removed" to LogLevel.INFO, "slow sync" to LogLevel.WARNING)
        assertEquals(expected, fixture.installed.entries)
    }

    @Test
    fun `should pass errors to remote log installed in logger`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException("callback failed")

        whenn()
        fixture.withLogger { LoggerRemoteLog.recordError("onSuccess", error) }

        then()
        assertEquals(listOf("onSuccess" to error), fixture.installed.errors)
    }

    @Test
    fun `should pass captured events to remote log installed in logger`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val event = stubRetenoLogEvent()

        whenn()
        fixture.withLogger { LoggerRemoteLog.recordEvent(event) }

        then()
        assertEquals(listOf(event), fixture.installed.events)
    }
}
