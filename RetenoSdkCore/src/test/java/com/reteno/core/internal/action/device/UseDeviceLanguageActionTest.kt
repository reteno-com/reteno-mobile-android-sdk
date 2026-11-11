package com.reteno.core.internal.action.device

import com.reteno.core.data.local.mappers.toDb
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

internal class UseDeviceLanguageActionTest {

    private class SutFixture(scope: TestScope) {
        val env = DeviceActionFixture(scope)
        val sut = UseDeviceLanguageAction(
            configRepository = env.configRepository,
            deviceLanguage = env.deviceLanguage,
            currentDevice = env.currentDevice,
            sync = env.sync,
            runner = env.traced.runner
        )
    }

    @Test
    fun `should forget explicit language and save device with detected language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.deviceLanguage.explicit = "de-AT"
        fixture.env.deviceLanguage.detected = "uk-UA"

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("use detected"), fixture.env.deviceLanguage.calls)
        val expected = stubDevice(fixture.env.deviceId, "token", true, languageCode = "uk-UA").toDb()
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(expected) }
    }

    @Test
    fun `should request data sync after saving the device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.deviceLanguage.explicit = "de-AT"

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.env.sync.requests)
    }

    @Test
    fun `should keep stored device when its language is already detected`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.deviceLanguage.explicit = "de-AT"
        val stored = stubDevice(fixture.env.deviceId, "token", true, languageCode = "en-US").toDb()
        every { fixture.env.devicesDb.getDevices(any()) } returns listOf(stored)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.env.devicesDb.insertDevice(any()) }
    }

    @Test
    fun `should leave device and sync untouched when no explicit language was set`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.env.devicesDb.insertDevice(any()) }
        assertEquals(0, fixture.env.sync.requests)
    }
}
