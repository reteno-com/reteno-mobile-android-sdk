package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.remote.model.device.DeviceCategoryRemote
import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.data.remote.model.device.DeviceRemote
import org.junit.Assert.assertEquals
import org.junit.Test


class DeviceMapperKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "valueDeviceId"
        private const val EXTERNAL_USER_ID = "valueExternalUserId"

        private const val PUSH_TOKEN = "valuePushToken"
        private val PUSH_SUBSCRIBED = BooleanDb.TRUE
        private const val PUSH_SUBSCRIBED_REMOTE = true
        private val CATEGORY = DeviceCategoryDb.TABLET
        private val CATEGORY_REMOTE = DeviceCategoryRemote.TABLET
        private val OS_TYPE = DeviceOsDb.ANDROID
        private val OS_TYPE_REMOTE = DeviceOsRemote.ANDROID
        private const val OS_VERSION = "valueOsVersion"
        private const val DEVICE_MODEL = "valueDeviceModel"
        private const val APP_VERSION = "valueAppVersion"
        private const val LANGUAGE_CODE = "valueLanguageCode"
        private const val TIME_ZONE = "valueTimeZone"
        private const val ADVERTISING_ID = "valueAdvertisingId"
        private const val EMAIL = "valueEmail"
        private const val PHONE = "valuePhone"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenDeviceDb_whenToRemote_thenDeviceRemoteReturned() {
        // Given
        val deviceDb = getDeviceDb()
        val expectedDeviceRemote = getDeviceRemote()

        // When
        val actualDeviceRemote = deviceDb.toRemote()

        // Then
        assertEquals(expectedDeviceRemote, actualDeviceRemote)
    }

    @Test
    fun givenDeviceOsDb_whenToRemote_thenDeviceOsRemoteReturned() {
        // Given
        val deviceOsDb = OS_TYPE
        val expectedDeviceOsRemote = OS_TYPE_REMOTE

        // When
        val actualDeviceOsRemote = deviceOsDb.toRemote()

        // Then
        assertEquals(expectedDeviceOsRemote, actualDeviceOsRemote)
    }

    @Test
    fun givenDeviceCategoryDb_whenToRemote_thenDeviceCategoryRemoteReturned() {
        // Given
        val deviceCategoryDb = CATEGORY
        val expectedDeviceCategoryRemote = CATEGORY_REMOTE

        // When
        val actualDeviceCategoryRemote = deviceCategoryDb.toRemote()

        // Then
        assertEquals(expectedDeviceCategoryRemote, actualDeviceCategoryRemote)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getDeviceDb() = DeviceDb(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        pushToken = PUSH_TOKEN,
        pushSubscribed = PUSH_SUBSCRIBED,
        category = CATEGORY,
        osType = OS_TYPE,
        osVersion = OS_VERSION,
        deviceModel = DEVICE_MODEL,
        appVersion = APP_VERSION,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        advertisingId = ADVERTISING_ID,
        email = EMAIL,
        phone = PHONE
    )

    private fun getDeviceRemote() = DeviceRemote(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        pushToken = PUSH_TOKEN,
        pushSubscribed = PUSH_SUBSCRIBED_REMOTE,
        category = CATEGORY_REMOTE,
        osType = OS_TYPE_REMOTE,
        osVersion = OS_VERSION,
        deviceModel = DEVICE_MODEL,
        appVersion = APP_VERSION,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        advertisingId = ADVERTISING_ID,
        phone = PHONE,
        email = EMAIL
    )
    // endregion helper methods --------------------------------------------------------------------
}