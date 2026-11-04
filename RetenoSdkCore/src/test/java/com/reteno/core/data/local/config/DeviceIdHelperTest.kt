package com.reteno.core.data.local.config

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.fixtures.FakeDeviceIdSources
import com.reteno.core.fixtures.STUB_ACCESS_KEY
import com.reteno.core.identification.DeviceIdProvider
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DeviceIdHelperTest {

    private class SutFixture(provider: DeviceIdProvider? = null) {
        val sources = FakeDeviceIdSources()
        val prefs = mockk<SharedPrefsManager>().also {
            every { it.getDeviceIdUuid() } returns "random-uuid"
            every { it.getLastStoredId() } returns "cached-body:suffix"
        }
        val config: RetenoConfig = RetenoConfig.Builder()
            .accessKey(STUB_ACCESS_KEY)
            .customDeviceIdProvider(provider)
            .build()
        val sut = DeviceIdHelper(sources, prefs) { config }
    }

    @Test
    fun `should identify device by android id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.ANDROID_ID)

        then()
        assertEquals(CURRENT.copy(idBody = "android-id", mode = DeviceIdMode.ANDROID_ID), deviceId)
    }

    @Test
    fun `should fall back to random uuid when android id is missing or unreadable`() = runUnitTest {
        given()
        val missing = SutFixture().apply { sources.androidId = { null } }
        val unreadable = SutFixture().apply { sources.androidId = { throw SecurityException("denied") } }

        whenn()
        val ids = listOf(missing, unreadable).map { it.sut.withDeviceIdMode(CURRENT, DeviceIdMode.ANDROID_ID) }

        then()
        val expected = CURRENT.copy(idBody = "random-uuid", mode = DeviceIdMode.RANDOM_UUID)
        assertEquals(listOf(expected, expected), ids)
    }

    @Test
    fun `should identify device by app set id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.APP_SET_ID)

        then()
        assertEquals(CURRENT.copy(idBody = "app-set-id", mode = DeviceIdMode.APP_SET_ID), deviceId)
    }

    @Test
    fun `should fall back to random uuid when app set id is not available`() = runUnitTest {
        given()
        val fixture = SutFixture().apply { sources.appSetId = { throw IllegalStateException("no play services") } }

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.APP_SET_ID)

        then()
        assertEquals(DeviceIdMode.RANDOM_UUID, deviceId.mode)
    }

    @Test
    fun `should identify device by stored random uuid`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.RANDOM_UUID)

        then()
        assertEquals(CURRENT.copy(idBody = "random-uuid", mode = DeviceIdMode.RANDOM_UUID), deviceId)
    }

    @Test
    fun `should identify device by id body of the last launch`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.CACHE)

        then()
        assertEquals(CURRENT.copy(idBody = "cached-body", mode = DeviceIdMode.CACHE), deviceId)
    }

    @Test
    fun `should identify device by id from host provider`() = runUnitTest {
        given()
        val fixture = SutFixture { "host-id" }

        whenn()
        val deviceId = fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.CLIENT_UUID)

        then()
        assertEquals(CURRENT.copy(idBody = "host-id", mode = DeviceIdMode.CLIENT_UUID), deviceId)
    }

    @Test
    fun `should wait for host provider to have an id`() = runUnitTest {
        given()
        var calls = 0
        val fixture = SutFixture { if (++calls < 3) null else "late-id" }
        val resolving = async { fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.CLIENT_UUID) }
        runCurrent()
        val doneBeforeWaiting = resolving.isCompleted

        whenn()
        advanceTimeBy(DeviceIdHelper.CLIENT_ID_POLL_MILLIS * 2 + 1)

        then()
        assertFalse(doneBeforeWaiting)
        assertEquals("late-id", resolving.await().idBody)
    }

    @Test
    fun `should fall back to random uuid without host provider or when it fails`() = runUnitTest {
        given()
        val withoutProvider = SutFixture()
        val failing = SutFixture { throw IllegalStateException("host crashed") }

        whenn()
        val modes = listOf(withoutProvider, failing).map {
            it.sut.withDeviceIdMode(CURRENT, DeviceIdMode.CLIENT_UUID).mode
        }

        then()
        assertEquals(listOf(DeviceIdMode.RANDOM_UUID, DeviceIdMode.RANDOM_UUID), modes)
    }

    @Test
    fun `should stop waiting for host provider when cancelled instead of falling back`() = runUnitTest {
        given()
        val fixture = SutFixture { null }
        val resolving = async { fixture.sut.withDeviceIdMode(CURRENT, DeviceIdMode.CLIENT_UUID) }
        runCurrent()

        whenn()
        resolving.cancel()
        runCurrent()

        then()
        assertTrue(resolving.isCancelled)
        verify(exactly = 0) { fixture.prefs.getDeviceIdUuid() }
    }

    private companion object {
        val CURRENT = DeviceId(idBody = "old", idSuffix = "suffix", externalId = "user-1", email = "a@b.c")
    }
}
