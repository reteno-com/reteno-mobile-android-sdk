package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.fixtures.FakeDeviceIdSources
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.internal.platform.FcmTokenSource
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConfigRepositoryImplTest {

    private class FakeFcmTokenSource : FcmTokenSource {
        override var isAutoInitEnabled = true
        var token: String? = "fresh-token"
        var fetches = 0

        override suspend fun fetchToken(): String? {
            fetches++
            return token
        }
    }

    private class SutFixture {
        var storedToken = ""
        val prefs = mockk<SharedPrefsManager>(relaxed = true).also {
            every { it.isNotificationsEnabled() } returns true
            every { it.getFcmToken() } answers { storedToken }
            every { it.saveFcmToken(any()) } answers { storedToken = firstArg() }
            every { it.getDeviceIdUuid() } returns "random-uuid"
        }
        val restConfig = RestConfig(prefs, DeviceIdHelper(FakeDeviceIdSources(), prefs) { stubConfig() })
        val tokens = FakeFcmTokenSource()
        val sut = ConfigRepositoryImpl(prefs, restConfig, tokens)
    }

    @Test
    fun `should keep user details of the device id in rest config`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.setExternalUserId("user-1")
        fixture.sut.setDeviceIdSuffix("tablet")
        fixture.sut.setUserEmail("a@b.c")
        fixture.sut.setUserPhone("+100")

        then()
        assertEquals(
            DeviceId(idBody = "", idSuffix = "tablet", externalId = "user-1", email = "a@b.c", phone = "+100"),
            fixture.sut.getDeviceId()
        )
    }

    @Test
    fun `should return device id at once when it is known`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.restConfig.initDeviceId(DeviceIdMode.RANDOM_UUID)

        whenn()
        val deviceId = fixture.sut.awaitForDeviceId()

        then()
        assertEquals("random-uuid", deviceId.idBody)
    }

    @Test
    fun `should wait for device id to become known`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiting = async { fixture.sut.awaitForDeviceId() }
        runCurrent()
        val doneBeforeKnown = waiting.isCompleted

        whenn()
        fixture.restConfig.initDeviceId(DeviceIdMode.RANDOM_UUID)
        advanceTimeBy(ConfigRepositoryImpl.DEVICE_ID_POLL_MILLIS + 1)

        then()
        assertFalse(doneBeforeKnown)
        assertEquals("random-uuid", waiting.await().idBody)
    }

    @Test
    fun `should use stored fcm token without asking firebase`() = runUnitTest {
        given()
        val fixture = SutFixture().apply { storedToken = "stored-token" }

        whenn()
        val token = fixture.sut.getFcmToken()

        then()
        assertEquals("stored-token", token)
        assertEquals(0, fixture.tokens.fetches)
    }

    @Test
    fun `should fetch and store fcm token when none is stored`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val token = fixture.sut.getFcmToken()

        then()
        assertEquals("fresh-token", token)
        assertEquals("fresh-token", fixture.sut.getStoredFcmToken())
    }

    @Test
    fun `should not store an empty fcm token firebase returns`() = runUnitTest {
        given()
        val fixture = SutFixture().apply { tokens.token = "" }

        whenn()
        val token = fixture.sut.getFcmToken()

        then()
        assertEquals("", token)
        verify(exactly = 0) { fixture.prefs.saveFcmToken(any()) }
    }

    @Test
    fun `should have no fcm token when firebase cannot provide one`() = runUnitTest {
        given()
        val fixture = SutFixture().apply { tokens.token = null }

        whenn()
        val token = fixture.sut.getFcmToken()

        then()
        assertEquals("", token)
        verify(exactly = 0) { fixture.prefs.saveFcmToken(any()) }
    }

    @Test
    fun `should clear fcm token when firebase auto init is off`() = runUnitTest {
        given()
        val fixture = SutFixture().apply { tokens.isAutoInitEnabled = false }

        whenn()
        val token = fixture.sut.getFcmToken()

        then()
        assertEquals("", token)
        assertEquals(0, fixture.tokens.fetches)
        verify(exactly = 1) { fixture.prefs.saveFcmToken("") }
    }

    @Test
    fun `should publish and store notifications state`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val initial = fixture.sut.notificationState.value

        whenn()
        fixture.sut.saveNotificationsEnabled(false)

        then()
        assertEquals(listOf(true, false), listOf(initial, fixture.sut.notificationState.value))
        verify(exactly = 1) { fixture.prefs.saveNotificationsEnabled(false) }
    }

    @Test
    fun `should read and store app settings in shared preferences`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.prefs.getDefaultNotificationChannel() } returns "{}"
        every { fixture.prefs.isDeviceRegistered() } returns true
        every { fixture.prefs.getAppVersion() } returns "1.0"
        every { fixture.prefs.getAppBuildNumber() } returns 7L

        whenn()
        fixture.sut.saveDefaultNotificationChannel("{\"id\":\"c\"}")
        fixture.sut.saveDeviceRegistered(true)
        fixture.sut.saveAppVersion("2.0")
        fixture.sut.saveAppBuildNumber(8L)
        val read = listOf(
            fixture.sut.getDefaultNotificationChannel(),
            fixture.sut.isDeviceRegistered(),
            fixture.sut.isNotificationsEnabled(),
            fixture.sut.getAppVersion(),
            fixture.sut.getAppBuildNumber()
        )

        then()
        assertEquals(listOf("{}", true, true, "1.0", 7L), read)
        verify(exactly = 1) { fixture.prefs.saveDefaultNotificationChannel("{\"id\":\"c\"}") }
        verify(exactly = 1) { fixture.prefs.saveDeviceRegistered(true) }
        verify(exactly = 1) { fixture.prefs.saveAppVersion("2.0") }
        verify(exactly = 1) { fixture.prefs.saveAppBuildNumber(8L) }
    }
}
