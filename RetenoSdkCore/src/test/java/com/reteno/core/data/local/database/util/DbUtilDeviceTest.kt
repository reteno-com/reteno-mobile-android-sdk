package com.reteno.core.data.local.database.util

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceOS
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import net.sqlcipher.Cursor
import org.junit.Assert.assertEquals
import org.junit.Test


class DbUtilDeviceTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "valueDeviceId"
        private const val EXTERNAL_USER_ID = "valueExternalUserId"

        private const val PUSH_TOKEN = "valuePushToken"
        private val PUSH_SUBSCRIBED = BooleanDb.TRUE
        private val CATEGORY = DeviceCategoryDb.TABLET
        private val OS_TYPE = DeviceOsDb.ANDROID
        private const val OS_VERSION = "valueOsVersion"
        private const val DEVICE_MODEL = "valueDeviceModel"
        private const val APP_VERSION = "valueAppVersion"
        private const val LANGUAGE_CODE = "valueLanguageCode"
        private const val TIME_ZONE = "valueTimeZone"
        private const val ADVERTISING_ID = "valueAdvertisingId"

        private const val COLUMN_INDEX_DEVICE_ID = 1
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 2
        private const val COLUMN_INDEX_PUSH_TOKEN = 3
        private const val COLUMN_INDEX_PUSH_SUBSCRIBED = 4
        private const val COLUMN_INDEX_CATEGORY = 5
        private const val COLUMN_INDEX_OS_TYPE = 6
        private const val COLUMN_INDEX_OS_VERSION = 7
        private const val COLUMN_INDEX_DEVICE_MODEL = 8
        private const val COLUMN_INDEX_APP_VERSION = 9
        private const val COLUMN_INDEX_LANGUAGE_CODE = 10
        private const val COLUMN_INDEX_TIME_ZONE = 12
        private const val COLUMN_INDEX_ADVERTISING_ID = 13
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private val contentValues = ContentValues()

    @MockK
    private lateinit var cursor: Cursor
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        contentValues.clear()
        mockColumnIndexes()
    }

    override fun after() {
        super.after()
        contentValues.clear()
        clearMocks(cursor)
    }

    @Test
    fun givenDeviceProvided_whenPutDevice_thenContentValuesUpdated() {
        // Given
        val device = DeviceDb(
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
            advertisingId = ADVERTISING_ID
        )
        val keySet = arrayOf(
            DeviceSchema.COLUMN_DEVICE_ID,
            DeviceSchema.COLUMN_EXTERNAL_USER_ID,
            DeviceSchema.COLUMN_PUSH_TOKEN,
            DeviceSchema.COLUMN_PUSH_SUBSCRIBED,
            DeviceSchema.COLUMN_CATEGORY,
            DeviceSchema.COLUMN_OS_TYPE,
            DeviceSchema.COLUMN_OS_VERSION,
            DeviceSchema.COLUMN_DEVICE_MODEL,
            DeviceSchema.COLUMN_APP_VERSION,
            DeviceSchema.COLUMN_LANGUAGE_CODE,
            DeviceSchema.COLUMN_TIMEZONE,
            DeviceSchema.COLUMN_ADVERTISING_ID
        )

        // When
        contentValues.putDevice(device)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(DeviceSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(DeviceSchema.COLUMN_EXTERNAL_USER_ID))
        assertEquals(PUSH_TOKEN, contentValues.get(DeviceSchema.COLUMN_PUSH_TOKEN))
        assertEquals(CATEGORY.toString(), contentValues.get(DeviceSchema.COLUMN_CATEGORY))
        assertEquals(OS_TYPE.toString(), contentValues.get(DeviceSchema.COLUMN_OS_TYPE))
        assertEquals(OS_VERSION, contentValues.get(DeviceSchema.COLUMN_OS_VERSION))
        assertEquals(DEVICE_MODEL, contentValues.get(DeviceSchema.COLUMN_DEVICE_MODEL))
        assertEquals(APP_VERSION, contentValues.get(DeviceSchema.COLUMN_APP_VERSION))
        assertEquals(LANGUAGE_CODE, contentValues.get(DeviceSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(DeviceSchema.COLUMN_TIMEZONE))
        assertEquals(ADVERTISING_ID, contentValues.get(DeviceSchema.COLUMN_ADVERTISING_ID))
    }

    @Test
    fun givenCursorWithDeviceProvided_whenGetDevice_thenDeviceReturned() {
        // Given
        mockDeviceFull()

        val expectedDevice = DeviceDb(
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
            advertisingId = ADVERTISING_ID
        )

        // When
        val actualDevice = cursor.getDevice()

        // Then
        assertEquals(expectedDevice, actualDevice)
    }

    @Test
    fun givenCursorWithDeviceIdOnly_whenGetDevice_thenDeviceReturned() {
        // Given
        mockDeviceDeviceIdOnly()

        val expectedDevice = Device(
            deviceId = DEVICE_ID,
            externalUserId = null,
            pushToken = null,
            pushSubscribed = null,
            category = Device.fetchDeviceCategory(),
            osType = DeviceOS.ANDROID,
            osVersion = null,
            deviceModel = null,
            appVersion = null,
            languageCode = null,
            timeZone = null,
            advertisingId = null
        ).toDb()

        // When
        val actualDevice = cursor.getDevice()

        // Then
        assertEquals(expectedDevice, actualDevice)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockDeviceFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_PUSH_TOKEN) } returns PUSH_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_PUSH_SUBSCRIBED) } returns PUSH_SUBSCRIBED.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_CATEGORY) } returns CATEGORY.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_OS_TYPE) } returns OS_TYPE.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_OS_VERSION) } returns OS_VERSION
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_MODEL) } returns DEVICE_MODEL
        every { cursor.getStringOrNull(COLUMN_INDEX_APP_VERSION) } returns APP_VERSION
        every { cursor.getStringOrNull(COLUMN_INDEX_LANGUAGE_CODE) } returns LANGUAGE_CODE
        every { cursor.getStringOrNull(COLUMN_INDEX_TIME_ZONE) } returns TIME_ZONE
        every { cursor.getStringOrNull(COLUMN_INDEX_ADVERTISING_ID) } returns ADVERTISING_ID
    }

    private fun mockDeviceDeviceIdOnly() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_PUSH_TOKEN) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_PUSH_SUBSCRIBED) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_PUSH_SUBSCRIBED) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_CATEGORY) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_OS_TYPE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_OS_VERSION) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_MODEL) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_APP_VERSION) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_LANGUAGE_CODE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_TIME_ZONE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_ADVERTISING_ID) } returns null
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_PUSH_TOKEN) } returns COLUMN_INDEX_PUSH_TOKEN
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_PUSH_SUBSCRIBED) } returns COLUMN_INDEX_PUSH_SUBSCRIBED
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_CATEGORY) } returns COLUMN_INDEX_CATEGORY
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_OS_TYPE) } returns COLUMN_INDEX_OS_TYPE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_OS_VERSION) } returns COLUMN_INDEX_OS_VERSION
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_MODEL) } returns COLUMN_INDEX_DEVICE_MODEL
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_APP_VERSION) } returns COLUMN_INDEX_APP_VERSION
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_TIMEZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_ADVERTISING_ID) } returns COLUMN_INDEX_ADVERTISING_ID
    }
    // endregion helper methods --------------------------------------------------------------------
}