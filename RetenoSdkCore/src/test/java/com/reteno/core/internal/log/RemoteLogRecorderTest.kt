package com.reteno.core.internal.log

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.RecordingLogEventQueue
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RemoteLogRecorderTest {

    private class SutFixture {
        val configRepository = mockk<ConfigRepository>()
        val platform = FakeAppPlatform()
        val queue = RecordingLogEventQueue()
        val sut = RemoteLogRecorder(lazy { configRepository }, platform, queue)

        init {
            every { configRepository.getDeviceId() } returns stubDeviceId()
        }
    }

    @Test
    fun `should queue message with level app package and device id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.record("rows removed", LogLevel.WARNING)

        then()
        val event = fixture.queue.events.single()
        assertEquals("rows removed", event.errorMessage)
        assertEquals(LogLevel.WARNING, event.logLevel)
        assertEquals(fixture.platform.origin.packageName, event.bundleId)
        assertEquals(stubDeviceId().id, event.deviceId)
    }

    @Test
    fun `should describe app and device once for every queued event`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.info("first")
        fixture.sut.info("second")

        then()
        val origin = fixture.platform.origin
        val expected = listOf(origin.osVersion, origin.appVersion, origin.deviceModel)
        val described = fixture.queue.events.map { listOf(it.osVersion, it.version, it.device) }
        assertEquals(listOf(expected, expected), described)
        assertEquals(1, fixture.platform.originReads)
    }

    @Test
    fun `should queue info and error messages with their levels`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.info("rows removed")
        fixture.sut.error("action failed")

        then()
        assertEquals(listOf(LogLevel.INFO, LogLevel.ERROR), fixture.queue.events.map { it.logLevel })
    }

    @Test
    fun `should drop interpolated values from queued message`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.record("setAnonymousUserAttributes(): attributes = [phone=+380501234567]", LogLevel.INFO)

        then()
        assertEquals("setAnonymousUserAttributes(): attributes", fixture.queue.events.single().errorMessage)
    }

    @Test
    fun `should queue error with redacted message and stack trace`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IllegalStateException(
            "request to https://mobile-api.reteno.com/api/v1/user failed, email = john@example.com"
        )

        whenn()
        val message = "getUser(). Removed invalid entry from database. user=UserDb(email=john@example.com)"
        fixture.sut.recordError(message, error)

        then()
        val event = fixture.queue.events.single()
        val saved = requireNotNull(event.errorMessage)
        val expectedPrefix = "getUser(). Removed invalid entry from database. user: " +
            "java.lang.IllegalStateException: request to <url> failed, email"
        assertTrue(saved.startsWith(expectedPrefix))
        assertFalse(saved.contains("john@example.com"))
        assertFalse(saved.contains("reteno.com"))
        assertEquals(LogLevel.ERROR, event.logLevel)
    }

    @Test
    fun `should mark device as uninitialized when device id is not available`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.configRepository.getDeviceId() } throws IllegalStateException("no device id yet")

        whenn()
        fixture.sut.info("before init")

        then()
        assertEquals("uninitialized", fixture.queue.events.single().deviceId)
    }

    @Test
    fun `should drop messages recorded while remote log is muted`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        RemoteLogGuard.muted { fixture.sut.error("log event could not be saved") }
        fixture.sut.info("after saving")

        then()
        assertEquals(listOf("after saving"), fixture.queue.events.map { it.errorMessage })
    }

    @Test
    fun `should not record again from inside its own queueing`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.queue.onSubmit = { fixture.sut.error("queue failed") }

        whenn()
        fixture.sut.info("first")

        then()
        assertEquals(listOf("first"), fixture.queue.events.map { it.errorMessage })
    }

    @Test
    fun `should not read device id before something is recorded`() = runUnitTest {
        given()
        var read = false
        val configRepository = lazy {
            read = true
            mockk<ConfigRepository>()
        }

        whenn()
        RemoteLogRecorder(configRepository, FakeAppPlatform(), RecordingLogEventQueue())

        then()
        assertFalse(read)
    }

    @Test
    fun `should keep fields of a captured event and replace package and device id`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val event = stubRetenoLogEvent(errorMessage = "bridge failed").copy(
            sdkVersion = "flutter-3.1.0",
            version = "9.9 (99)",
            bundleId = "com.other.app",
            deviceId = "other-device"
        )

        whenn()
        fixture.sut.recordEvent(event)

        then()
        val expected = event.copy(bundleId = fixture.platform.origin.packageName, deviceId = stubDeviceId().id)
        assertEquals(expected, fixture.queue.events.single())
    }

    @Test
    fun `should fill app version of a captured event that has none`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.recordEvent(stubRetenoLogEvent().copy(version = null))

        then()
        assertEquals(fixture.platform.origin.appVersion, fixture.queue.events.single().version)
    }

    @Test
    fun `should drop interpolated values from captured event message`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.recordEvent(stubRetenoLogEvent(errorMessage = "token = [secret-token]"))
        fixture.sut.recordEvent(stubRetenoLogEvent(errorMessage = null))

        then()
        assertEquals(listOf("token", null), fixture.queue.events.map { it.errorMessage })
    }

    @Test
    fun `should drop captured events while remote log is muted`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        RemoteLogGuard.muted { fixture.sut.recordEvent(stubRetenoLogEvent()) }

        then()
        assertTrue(fixture.queue.events.isEmpty())
    }
}
