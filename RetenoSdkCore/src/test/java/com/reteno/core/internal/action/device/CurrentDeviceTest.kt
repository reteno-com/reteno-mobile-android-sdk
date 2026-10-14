package com.reteno.core.internal.action.device

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubDeviceId
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

internal class CurrentDeviceTest {

    private class SutFixture(scope: TestScope) {
        val configRepository = mockk<ConfigRepository>()
        val devicesDb = mockk<RetenoDatabaseManagerDevice>()
        val deviceId = stubDeviceId()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = CurrentDevice(configRepository, devicesDb, FakeAppPlatform(), dispatchers)

        init {
            every { configRepository.getDeviceId() } returns deviceId
            every { configRepository.isNotificationsEnabled() } returns true
            justRun { devicesDb.insertDevice(any()) }
        }
    }

    @Test
    fun `should save device built from current identity token and subscription`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()

        whenn()
        val saved = fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "new-token", pushSubscribed = false)
        }.getOrThrow()

        then()
        assertTrue(saved)
        verify(exactly = 1) { fixture.devicesDb.insertDevice(stubDevice(fixture.deviceId, "new-token", false).toDb()) }
    }

    @Test
    fun `should use stored notification state when subscription is not given`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()
        every { fixture.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token")
        }

        then()
        verify(exactly = 1) { fixture.devicesDb.insertDevice(stubDevice(fixture.deviceId, "token", false).toDb()) }
    }

    @Test
    fun `should skip device identical to a stored one regardless of row id time and sync flag`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val stored = stubDevice(fixture.deviceId, "token", true).toDb().copy(
            rowId = "9",
            createdAt = 123L,
            isSynchronizedWithBackend = BooleanDb.TRUE
        )
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stored)

        whenn()
        val saved = fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token", pushSubscribed = true)
        }.getOrThrow()

        then()
        assertFalse(saved)
        verify(exactly = 0) { fixture.devicesDb.insertDevice(any()) }
    }

    @Test
    fun `should load stored devices from local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.devicesDb.getDevices(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token")
        }

        then()
        assertTrue(loadedOnIo)
    }

    @Test
    fun `should insert device into local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var insertedOnIo = false
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()
        every { fixture.devicesDb.insertDevice(any()) } answers {
            insertedOnIo = fixture.dispatchers.markedIo.isRunningHere
        }

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token")
        }

        then()
        assertTrue(insertedOnIo)
    }

    @Test
    fun `should trace saved device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token")
        }

        then()
        val savedLines = fixture.traced.sink.lines.count {
            it.contains("✓ save current device to local database") && it.contains("saved")
        }
        assertEquals(1, savedLines)
    }

    @Test
    fun `should not write push token to trace`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns emptyList()

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "secret-token")
        }

        then()
        assertFalse(fixture.traced.sink.lines.any { it.contains("secret-token") })
    }

    @Test
    fun `should trace unchanged device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDevice(fixture.deviceId, "token", true).toDb())

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.save(this, pushToken = "token")
        }

        then()
        val unchangedLines = fixture.traced.sink.lines.count {
            it.contains("save current device to local database") && it.contains("unchanged")
        }
        assertEquals(1, unchangedLines)
    }
}
