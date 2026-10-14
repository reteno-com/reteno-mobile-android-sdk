package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RegisterDeviceActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        val sut = RegisterDeviceAction(
            configRepository = env.configRepository,
            currentDevice = env.currentDevice,
            sync = env.sync,
            session = env.session,
            runner = env.traced.runner
        )
    }

    @Test
    fun `should save current device with push token and sync contact data immediately`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        val result = fixture.sut().getOrThrow()

        then()
        assertEquals(SyncResult.COMPLETED, result)
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", true).toDb())
        }
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should return aborted sync result`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"
        fixture.env.sync.result = SyncResult.ABORTED

        whenn()
        val result = fixture.sut().getOrThrow()

        then()
        assertEquals(SyncResult.ABORTED, result)
        assertTrue(fixture.env.traced.sink.lines.last().contains("RegisterDevice ✗ ABORTED"))
    }

    @Test
    fun `should fail without sending when push token cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery {
            fixture.env.configRepository.getFcmToken()
        } throws IllegalStateException("Default FirebaseApp is not initialized")

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.env.sync.immediateScopes.isEmpty())
        assertFalse(fixture.env.session.isDeviceSent)
        assertEquals("read push token", fixture.env.traced.reports.single().step)
    }

    @Test
    fun `should trace push token redacted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "secret-push-token-1234"

        whenn()
        fixture.sut()

        then()
        val lines = fixture.env.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ read push token") && it.contains("…1234") })
        assertTrue(lines.any { it.contains("✓ send device and user to server") })
        assertFalse(lines.any { it.contains("secret-push-token") })
    }
}
