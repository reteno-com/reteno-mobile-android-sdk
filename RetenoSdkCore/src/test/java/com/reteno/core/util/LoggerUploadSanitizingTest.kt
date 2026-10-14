package com.reteno.core.util

import android.app.Application
import com.reteno.core.Reteno
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class LoggerUploadSanitizingTest {

    private class SutFixture {
        val saved = slot<RetenoLogEvent>()
        val sdk = mockk<RetenoInternalImpl>().also {
            every { it.application } returns RuntimeEnvironment.getApplication() as Application
            every { it.getDeviceId() } returns "device-body"
            every { it.logRetenoEvent(capture(saved)) } returns Unit
        }

        fun <T> withSdk(block: () -> T): T {
            val previous = Reteno.instanceInternal
            Reteno.instanceInternal = sdk
            try {
                return block()
            } finally {
                Reteno.instanceInternal = previous
            }
        }
    }

    @Test
    fun `should persist error log without interpolated values or urls`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException(
            "request to https://mobile-api.reteno.com/api/v1/user failed, email = john@example.com"
        )
        val loggedMessage = "getUser(). Removed invalid entry from database. user=UserDb(email=john@example.com)"

        whenn()
        fixture.withSdk { Logger.e("Tag", loggedMessage, error) }

        then()
        val message = requireNotNull(fixture.saved.captured.errorMessage)
        val expectedPrefix = "getUser(). Removed invalid entry from database. user: " +
            "java.lang.IllegalStateException: request to <url> failed, email"
        assertTrue(message.startsWith(expectedPrefix))
        assertFalse(message.contains("john@example.com"))
        assertFalse(message.contains("reteno.com"))
        assertEquals(LogLevel.ERROR, fixture.saved.captured.logLevel)
    }

    @Test
    fun `should persist captured message without interpolated values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withSdk { Logger.captureMessage("setAnonymousUserAttributes(): attributes = [phone=+380501234567]") }

        then()
        assertEquals("setAnonymousUserAttributes(): attributes", fixture.saved.captured.errorMessage)
    }

    @Test
    fun `should persist captured event without interpolated values`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val event = fixture.withSdk {
            RetenoLogEvent(logLevel = LogLevel.INFO, errorMessage = "token = [secret-token]")
        }

        whenn()
        fixture.withSdk { Logger.captureEvent(event) }

        then()
        assertEquals("token", fixture.saved.captured.errorMessage)
    }
}
