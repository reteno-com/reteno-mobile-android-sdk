package com.reteno.core._interop

import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TestErrorInternalTest {

    @Test
    fun `should record test error through sdk error logging`() = runUnitTest {
        given()
        val remoteLog = FakeRemoteLog()

        whenn()
        try {
            Logger.install(FakeLogSink(), remoteLog)
            TestErrorInternal.logTestError()
        } finally {
            Logger.install(AndroidLogSink(), null)
        }

        then()
        val (message, error) = remoteLog.errors.single()
        assertEquals("logTestError(): ", message)
        assertTrue(error is NullPointerException)
    }
}
