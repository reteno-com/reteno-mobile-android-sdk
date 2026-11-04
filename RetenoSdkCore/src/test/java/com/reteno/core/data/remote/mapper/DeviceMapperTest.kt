package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.remote.model.device.DeviceCategoryRemote
import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.fixtures.stubDeviceDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class DeviceMapperTest {

    @Test
    fun `should send every stored device field`() = runUnitTest {
        given()
        val device = stubDeviceDb().copy(advertisingId = "ad-1", email = "a@b.c", phone = "+100")

        whenn()
        val remote = device.toRemote()

        then()
        val expected = listOf(
            "device-body", "external-user", "token", true, DeviceCategoryRemote.MOBILE, DeviceOsRemote.ANDROID,
            "14", "Pixel", "1.0", "en", "UTC", "ad-1", "a@b.c", "+100"
        )
        val actual = listOf(
            remote.deviceId, remote.externalUserId, remote.pushToken, remote.pushSubscribed, remote.category,
            remote.osType, remote.osVersion, remote.deviceModel, remote.appVersion, remote.languageCode,
            remote.timeZone, remote.advertisingId, remote.email, remote.phone
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `should send unknown push subscription as absent`() = runUnitTest {
        given()
        val device = stubDeviceDb().copy(pushSubscribed = null)

        whenn()
        val remote = device.toRemote()

        then()
        assertNull(remote.pushSubscribed)
    }

    @Test
    fun `should send push subscription switched off`() = runUnitTest {
        given()
        val device = stubDeviceDb().copy(pushSubscribed = BooleanDb.FALSE)

        whenn()
        val remote = device.toRemote()

        then()
        assertEquals(false, remote.pushSubscribed)
    }

    @Test
    fun `should send every os and category`() = runUnitTest {
        given()
        val systems = DeviceOsDb.values().toList()
        val categories = DeviceCategoryDb.values().toList()

        whenn()
        val mapped = systems.map { it.toRemote() } to categories.map { it.toRemote() }

        then()
        assertEquals(listOf(DeviceOsRemote.ANDROID, DeviceOsRemote.IOS), mapped.first)
        assertEquals(listOf(DeviceCategoryRemote.MOBILE, DeviceCategoryRemote.TABLET), mapped.second)
    }
}
