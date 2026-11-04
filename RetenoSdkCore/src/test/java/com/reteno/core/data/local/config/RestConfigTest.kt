package com.reteno.core.data.local.config

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.fixtures.FakeDeviceIdSources
import com.reteno.core.fixtures.stubConfig
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RestConfigTest {

    private class SutFixture {
        val prefs = mockk<SharedPrefsManager>().also {
            every { it.getExternalUserId() } returns "user-1"
            every { it.getDeviceIdSuffix() } returns "suffix"
            every { it.getEmail() } returns "a@b.c"
            every { it.getPhone() } returns "+100"
            justRun { it.saveDeviceIdSuffix(any()) }
            justRun { it.saveExternalUserId(any()) }
            justRun { it.saveDeviceEmail(any()) }
            justRun { it.saveDevicePhone(any()) }
        }
        val sut = RestConfig(prefs, DeviceIdHelper(FakeDeviceIdSources(), prefs) { stubConfig() })
    }

    @Test
    fun `should start without a device id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val deviceId = fixture.sut.deviceId

        then()
        assertEquals("", deviceId.id)
    }

    @Test
    fun `should identify device in the chosen mode with stored user details`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.initDeviceId(DeviceIdMode.ANDROID_ID)

        then()
        val expected = DeviceId(
            idBody = "android-id",
            idSuffix = "suffix",
            externalId = "user-1",
            mode = DeviceIdMode.ANDROID_ID,
            email = "a@b.c",
            phone = "+100"
        )
        assertEquals(expected, fixture.sut.deviceId)
    }

    @Test
    fun `should store and apply device id suffix`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.setDeviceIdSuffix("tablet")

        then()
        verify(exactly = 1) { fixture.prefs.saveDeviceIdSuffix("tablet") }
        assertEquals("tablet", fixture.sut.deviceId.idSuffix)
    }

    @Test
    fun `should store external user id and treat a blank one as empty`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val applied = listOf("user-2", "  ", null).map {
            fixture.sut.setExternalUserId(it)
            fixture.sut.deviceId.externalId
        }

        then()
        assertEquals(listOf("user-2", "", null), applied)
        verify(exactly = 1) { fixture.prefs.saveExternalUserId("  ") }
    }

    @Test
    fun `should store and apply email and phone`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.setDeviceEmail("new@b.c")
        fixture.sut.setDevicePhone("+200")

        then()
        verify(exactly = 1) { fixture.prefs.saveDeviceEmail("new@b.c") }
        verify(exactly = 1) { fixture.prefs.saveDevicePhone("+200") }
        assertEquals(listOf("new@b.c", "+200"), listOf(fixture.sut.deviceId.email, fixture.sut.deviceId.phone))
    }
}
