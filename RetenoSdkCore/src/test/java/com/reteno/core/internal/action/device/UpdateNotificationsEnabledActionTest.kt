package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.recordingLogEvent
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UpdateNotificationsEnabledActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        val logged = mutableListOf<Event>()
        val lifecycleEvents = LifecycleEvents(
            LifecycleEventFilter(LifecycleTrackingOptions.ALL),
            recordingLogEvent(logged)
        )
        val sut = UpdateNotificationsEnabledAction(
            configRepository = env.configRepository,
            currentDevice = env.currentDevice,
            sync = env.sync,
            session = env.session,
            lifecycleEvents = lifecycleEvents,
            runner = env.traced.runner
        )

        init {
            justRun { env.configRepository.saveNotificationsEnabled(any()) }
            coEvery { env.configRepository.getFcmToken() } returns "token"
        }
    }

    @Test
    fun `should not save the device again when push reports the state it was registered with`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val stored = mutableListOf<DeviceDb>()
        every { fixture.env.devicesDb.getDevices(any()) } answers { stored.toList() }
        every { fixture.env.devicesDb.insertDevice(any()) } answers { stored += firstArg<DeviceDb>() }
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false
        fixture.env.platform.notificationsEnabled = true
        fixture.env.traced.runner("RegisterDevice") {
            fixture.env.currentDevice.save(this, pushToken = "token")
        }

        whenn()
        fixture.sut(true)

        then()
        assertEquals(1, stored.size)
    }

    @Test
    fun `should ignore unchanged notification state`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(true)

        then()
        verify(exactly = 0) { fixture.env.configRepository.saveNotificationsEnabled(any()) }
        assertEquals(0, fixture.env.sync.requests)
        assertFalse(fixture.env.session.isDeviceSent)
        assertTrue(fixture.logged.isEmpty())
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ notifications state unchanged") })
    }

    @Test
    fun `should save disabled state when notifications become disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(false)

        then()
        verify(exactly = 1) { fixture.env.configRepository.saveNotificationsEnabled(false) }
    }

    @Test
    fun `should save device as not push subscribed when notifications become disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(false)

        then()
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", false).toDb())
        }
    }

    @Test
    fun `should save enabled state when notifications become enabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.sut(true)

        then()
        verify(exactly = 1) { fixture.env.configRepository.saveNotificationsEnabled(true) }
    }

    @Test
    fun `should save device as push subscribed when notifications become enabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.sut(true)

        then()
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", true).toDb())
        }
    }

    @Test
    fun `should request data sync when notifications state changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(false)

        then()
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should mark device as sent this session when notifications state changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(false)

        then()
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should trace requested state`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.sut(true)

        then()
        val lines = fixture.env.traced.sink.lines
        assertTrue(lines.first().contains("UpdateNotificationsEnabled  enabled=true"))
        assertTrue(lines.any { it.contains("✓ save notifications state to local settings") })
    }

    @Test
    fun `should log push unsubscribed when notifications become disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut(false)

        then()
        assertEquals(listOf("PushNotificationsUnsubscribed"), fixture.logged.map { it.eventTypeKey })
    }

    @Test
    fun `should log push subscribed when notifications become enabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.sut(true)

        then()
        assertEquals(listOf("PushNotificationsSubscribed"), fixture.logged.map { it.eventTypeKey })
    }
}
