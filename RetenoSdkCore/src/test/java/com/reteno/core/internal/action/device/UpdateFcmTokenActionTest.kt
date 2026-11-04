package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.fixtures.stubDevice
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UpdateFcmTokenActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        val sut = UpdateFcmTokenAction(
            configRepository = env.configRepository,
            currentDevice = env.currentDevice,
            sync = env.sync,
            session = env.session,
            runner = env.traced.runner
        )

        init {
            justRun { env.configRepository.saveFcmToken(any()) }
        }
    }

    @Test
    fun `should ignore token equal to stored one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.getStoredFcmToken() } returns "same-token"

        whenn()
        fixture.sut("same-token")

        then()
        verify(exactly = 0) { fixture.env.configRepository.saveFcmToken(any()) }
        assertEquals(0, fixture.env.sync.requests)
        assertFalse(fixture.env.session.isDeviceSent)
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ push token unchanged") })
    }

    @Test
    fun `should store new token save device with it and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.getStoredFcmToken() } returns "old-token"

        whenn()
        fixture.sut("new-token")

        then()
        verify(exactly = 1) { fixture.env.configRepository.saveFcmToken("new-token") }
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "new-token", true).toDb())
        }
        assertEquals(1, fixture.env.sync.requests)
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should compare with stored token without fetching a fresh one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.getStoredFcmToken() } returns ""

        whenn()
        fixture.sut("new-token")

        then()
        coVerify(exactly = 0) { fixture.env.configRepository.getFcmToken() }
        verify(exactly = 1) { fixture.env.configRepository.saveFcmToken("new-token") }
    }

    @Test
    fun `should never write push tokens to trace`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.getStoredFcmToken() } returns "old-secret-token"

        whenn()
        fixture.sut("new-secret-token-9876")

        then()
        val lines = fixture.env.traced.sink.lines
        assertTrue(lines.first().contains("UpdateFcmToken  token=…9876"))
        assertFalse(lines.any { it.contains("secret-token") })
    }
}
