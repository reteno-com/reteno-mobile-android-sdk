package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubDeviceDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EnsureDeviceRegisteredThisSessionActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        val sut = EnsureDeviceRegisteredThisSessionAction(
            env.configRepository, env.devicesDb, env.currentDevice, env.sync, env.session, env.dispatchers, env.traced.runner
        )
    }

    @Test
    fun `should do nothing when device was already sent this session`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.session.markDeviceSent()

        whenn()
        fixture.sut()

        then()
        assertEquals(0, fixture.env.sync.requests)
        coVerify(exactly = 0) { fixture.env.configRepository.getFcmToken() }
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ device already sent this session") })
    }

    @Test
    fun `should forget synced devices save current one and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubDeviceDb(rowId = "1", synced = BooleanDb.TRUE)
        val pending = stubDeviceDb(rowId = "2", pushToken = "older-token")
        every { fixture.env.devicesDb.getDevices(any()) } returnsMany listOf(listOf(synced, pending), listOf(pending))
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.env.devicesDb.deleteDevices(listOf(synced)) }
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", true).toDb()) }
        assertEquals(1, fixture.env.sync.requests)
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should not delete anything when there is no synced device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.env.devicesDb.deleteDevices(any()) }
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should access local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.env.devicesDb.getDevices(any()) } answers {
            loadedOnIo = fixture.env.dispatchers.markedIo.isRunningHere
            emptyList()
        }
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("✓ forget synced devices in local database") })
    }
}
