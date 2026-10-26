package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.STUB_CREATED_AT
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubDeviceDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PushDeviceStepTest {

    private class SutFixture(scope: TestScope) {
        val devicesDb = mockk<RetenoDatabaseManagerDevice>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var registeredCount = 0
        private val configRepository = mockk<ConfigRepository>().also {
            every { it.saveDeviceRegistered(true) } answers { registeredCount++ }
        }
        val sut = PushDeviceStep(devicesDb, rest.api(dispatchers), configRepository, dispatchers, traced.runner)

        init {
            justRun { devicesDb.deleteDevices(any()) }
            justRun { devicesDb.insertDevice(any()) }
            every { devicesDb.getUnSyncedDeviceCount() } returns 0L
        }
    }

    @Test
    fun `should report nothing to send without request when there are no devices`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should report nothing to send without request when every device is already synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb(synced = BooleanDb.TRUE))

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 0) { fixture.devicesDb.deleteDevices(any()) }
    }

    @Test
    fun `should send latest unsynced device when no device was synced before`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val older = stubDeviceDb(rowId = "1", createdAt = STUB_CREATED_AT, pushToken = "old")
        val latest = stubDeviceDb(rowId = "2", createdAt = STUB_CREATED_AT + 1, pushToken = "new")
        every { fixture.devicesDb.getDevices(any()) } returns listOf(older, latest)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.MobileApi.Device, latest.toRemote().toJson())),
            fixture.rest.requests
        )
    }

    @Test
    fun `should send device under its wire field names`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())

        whenn()
        fixture.sut()

        then()
        assertEquals(DEVICE_JSON, fixture.rest.requests.single().body)
    }

    @Test
    fun `should keep only latest device marked as synced after successful send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubDeviceDb(rowId = "1", pushToken = "old", synced = BooleanDb.TRUE)
        val latest = stubDeviceDb(rowId = "2", createdAt = STUB_CREATED_AT + 1, pushToken = "new")
        every { fixture.devicesDb.getDevices(any()) } returns listOf(synced, latest)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.devicesDb.deleteDevices(listOf(synced, latest)) }
        verify(exactly = 1) { fixture.devicesDb.insertDevice(latest.copy(isSynchronizedWithBackend = BooleanDb.TRUE)) }
        assertEquals(1, fixture.registeredCount)
    }

    @Test
    fun `should not send device identical to recently synced one but mark it synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubDeviceDb(rowId = "1", synced = BooleanDb.TRUE)
        val same = stubDeviceDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(synced, same)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.devicesDb.insertDevice(same.copy(isSynchronizedWithBackend = BooleanDb.TRUE)) }
        assertEquals(0, fixture.registeredCount)
    }

    @Test
    fun `should send identical device when synced one is outdated`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubDeviceDb(rowId = "1", synced = BooleanDb.TRUE)
        val same = stubDeviceDb(rowId = "2", createdAt = STUB_CREATED_AT + 10 * 60_000)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(synced, same)

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should send device that differs from recently synced one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubDeviceDb(rowId = "1", pushToken = "old", synced = BooleanDb.TRUE)
        val changed = stubDeviceDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000, pushToken = "new")
        every { fixture.devicesDb.getDevices(any()) } returns listOf(synced, changed)

        whenn()
        fixture.sut()

        then()
        assertEquals(changed.toRemote().toJson(), fixture.rest.requests.single().body)
    }

    @Test
    fun `should abort and keep devices untouched when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())
        fixture.rest.failWith(503)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.devicesDb.deleteDevices(any()) }
        assertEquals(0, fixture.registeredCount)
    }

    @Test
    fun `should abort when request gets no response`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())
        fixture.rest.failOffline()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
    }

    @Test
    fun `should drop rejected device and continue when server fails with non repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val device = stubDeviceDb()
        every { fixture.devicesDb.getDevices(any()) } returns listOf(device)
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.devicesDb.insertDevice(device.copy(isSynchronizedWithBackend = BooleanDb.TRUE)) }
        assertEquals(0, fixture.registeredCount)
    }

    @Test
    fun `should send again when new unsynced device appeared during send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returnsMany listOf(
            listOf(stubDeviceDb(rowId = "1", pushToken = "first")),
            listOf(stubDeviceDb(rowId = "2", pushToken = "second"))
        )
        every { fixture.devicesDb.getUnSyncedDeviceCount() } returnsMany listOf(1L, 0L)

        whenn()
        fixture.sut()

        then()
        assertEquals(2, fixture.rest.requests.size)
    }

    @Test
    fun `should continue after sending device when a later round finds nothing to send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returnsMany listOf(listOf(stubDeviceDb()), emptyList())
        every { fixture.devicesDb.getUnSyncedDeviceCount() } returns 1L

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(1, fixture.rest.requests.size)
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should stop after limited number of rounds`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())
        every { fixture.devicesDb.getUnSyncedDeviceCount() } returns 1L

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(PushDeviceStep.MAX_ROUNDS, fixture.rest.requests.size)
    }

    @Test
    fun `should access local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.devicesDb.getDevices(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
    }

    @Test
    fun `should trace steps without push token`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb(pushToken = "secret-push-token"))

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ load devices from local database") && it.contains("count=1") })
        assertTrue(lines.any { it.contains("✓ send device to server") && it.contains("ok") })
        assertTrue(lines.any { it.contains("✓ mark device as synced in local database") })
        assertTrue(lines.any { it.contains("✓ save device registered flag to local settings") })
        assertTrue(lines.last().contains("PushDevice ✓ CONTINUE"))
        assertFalse(lines.any { it.contains("secret-push-token") })
    }

    companion object {
        const val DEVICE_JSON = """{"deviceId":"device-body","externalUserId":"external-user","pushToken":"token",""" +
            """"pushSubscribed":true,"category":"MOBILE","osType":"ANDROID","osVersion":"14",""" +
            """"deviceModel":"Pixel","appVersion":"1.0","languageCode":"en","timeZone":"UTC"}"""
    }
}
