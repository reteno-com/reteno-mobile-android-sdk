package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.fixtures.stubDevice
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RefreshDeviceLanguageActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        var readOnIo: Boolean? = null
        val sut = RefreshDeviceLanguageAction(
            configRepository = env.configRepository,
            devicesDb = env.devicesDb,
            deviceLanguage = env.deviceLanguage,
            currentDevice = env.currentDevice,
            sync = env.sync,
            dispatchers = env.dispatchers,
            runner = env.traced.runner
        )

        fun savedDevices(vararg languageCodes: String) {
            val devices = languageCodes.mapIndexed { index, languageCode ->
                savedDevice(languageCode, rowId = "${index + 1}")
            }
            storedDevices(devices)
        }

        fun savedDevice(languageCode: String, rowId: String?) =
            stubDevice(env.deviceId, "token", true, languageCode).toDb().copy(rowId = rowId, createdAt = SAME_SECOND)

        fun storedDevices(devices: List<DeviceDb>) {
            every { env.devicesDb.getDevices(any()) } answers {
                readOnIo = env.dispatchers.markedIo.isRunningHere
                devices
            }
        }
    }

    @Test
    fun `should save device with the resolved language when the last saved device has another one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.savedDevices("uk-UA")
        fixture.env.deviceLanguage.detected = "en-US"

        whenn()
        fixture.sut()

        then()
        val expected = stubDevice(fixture.env.deviceId, "token", true, languageCode = "en-US").toDb()
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(expected) }
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should compare with the device saved last within the same second`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.savedDevices("de-DE", "en-US")
        fixture.env.deviceLanguage.detected = "en-US"

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.env.devicesDb.insertDevice(any()) }
        assertEquals(0, fixture.env.sync.requests)
    }

    @Test
    fun `should treat a saved device without row id as the oldest`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val devices = listOf(
            fixture.savedDevice("en-US", rowId = null),
            fixture.savedDevice("de-DE", rowId = "1")
        )
        fixture.storedDevices(devices)
        fixture.env.deviceLanguage.detected = "en-US"

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should save device when no device is saved yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.savedDevices()

        whenn()
        fixture.sut()

        then()
        val expected = stubDevice(fixture.env.deviceId, "token", true, languageCode = "en-US").toDb()
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(expected) }
    }

    @Test
    fun `should read saved devices on the io thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.savedDevices("en-US")

        whenn()
        fixture.sut()

        then()
        assertEquals(true, fixture.readOnIo)
    }

    @Test
    fun `should print one line when the language is unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.savedDevices("en-US")

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.env.traced.sink.lines.size)
    }

    private companion object {
        const val SAME_SECOND = 1_700_000_000_000L
    }
}
