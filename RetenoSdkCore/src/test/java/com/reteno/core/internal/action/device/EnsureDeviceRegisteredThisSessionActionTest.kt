package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubDeviceDb
import com.reteno.core.internal.action.trace.TraceFormatter
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
            configRepository = env.configRepository,
            devicesDb = env.devicesDb,
            currentDevice = env.currentDevice,
            sync = env.sync,
            session = env.session,
            dispatchers = env.dispatchers,
            runner = env.traced.runner
        )

        init {
            coEvery { env.configRepository.getFcmToken() } returns "token"
        }

        fun storeSyncedAndPendingDevices(): DeviceDb {
            val synced = stubDeviceDb(rowId = "1", synced = BooleanDb.TRUE)
            val pending = stubDeviceDb(rowId = "2", pushToken = "older-token")
            every { env.devicesDb.getDevices(any()) } returnsMany listOf(listOf(synced, pending), listOf(pending))
            return synced
        }
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
    fun `should delete synced devices from local database`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = fixture.storeSyncedAndPendingDevices()

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.env.devicesDb.deleteDevices(listOf(synced)) }
    }

    @Test
    fun `should save current device with stored push token`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.storeSyncedAndPendingDevices()

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", true).toDb())
        }
    }

    @Test
    fun `should request data sync after saving current device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should mark device as sent this session`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should not delete anything when there is no synced device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.env.devicesDb.deleteDevices(any()) }
    }

    @Test
    fun `should load devices to forget from local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val loadsOnIo = mutableListOf<Boolean>()
        every { fixture.env.devicesDb.getDevices(any()) } answers {
            loadsOnIo += fixture.env.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadsOnIo.first())
    }

    @Test
    fun `should delete synced devices from local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.storeSyncedAndPendingDevices()
        var deletedOnIo = false
        every { fixture.env.devicesDb.deleteDevices(any()) } answers {
            deletedOnIo = fixture.env.dispatchers.markedIo.isRunningHere
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(deletedOnIo)
    }

    @Test
    fun `should trace number of forgotten synced devices`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.storeSyncedAndPendingDevices()

        whenn()
        fixture.sut()

        then()
        val expected = TraceFormatter.stepSuccess(0, "r1", "forget synced devices in local database", "count=1", 0)
        assertTrue(expected in fixture.env.traced.sink.lines)
    }
}
