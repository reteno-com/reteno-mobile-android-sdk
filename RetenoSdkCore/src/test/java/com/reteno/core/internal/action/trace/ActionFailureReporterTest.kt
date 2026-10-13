package com.reteno.core.internal.action.trace

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ActionFailureReporterTest {

    private class SutFixture {
        val captured = mutableListOf<String>()
        val sut = RemoteLogFailureReporter(capture = { captured += it })
    }

    @Test
    fun `should capture message with action step and redacted error`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IOException("cannot reach https://mobile-api.reteno.com/api/v1/device")

        whenn()
        fixture.sut.report("Sync", "send device to server", error)

        then()
        assertEquals(listOf("Action Sync failed at step 'send device to server': IOException: cannot reach <url>"), fixture.captured)
    }

    @Test
    fun `should capture message without step`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException("broken")

        whenn()
        fixture.sut.report("LogEvent", null, error)

        then()
        assertEquals(listOf("Action LogEvent failed: IllegalStateException: broken"), fixture.captured)
    }

    @Test
    fun `should ignore failures when reporting disabled`() = runUnitTest {
        given()
        val sut = NoFailureReporting

        whenn()
        val outcome = runCatching { sut.report("LogRetenoEvent", null, IllegalStateException()) }

        then()
        assertTrue(outcome.isSuccess)
    }
}
