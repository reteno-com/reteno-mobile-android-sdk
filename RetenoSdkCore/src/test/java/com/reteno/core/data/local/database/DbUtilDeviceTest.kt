package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import io.mockk.*
import net.sqlcipher.Cursor
import org.junit.Assert.assertEquals
import org.junit.Test
import io.mockk.impl.annotations.MockK


class DbUtilDeviceTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "valueDeviceId"
        private const val EXTERNAL_USER_ID = "valueExternalUserId"

        private const val PUSH_TOKEN = "valuePushToken"
        private val CATEGORY = DeviceCategory.TABLET
        private val OS_TYPE = DeviceOS.ANDROID
        private const val OS_VERSION = "valueOsVersion"
        private const val DEVICE_MODEL = "valueDeviceModel"
        private const val APP_VERSION = "valueAppVersion"
        private const val LANGUAGE_CODE = "valueLanguageCode"
        private const val TIME_ZONE = "valueTimeZone"
        private const val ADVERTISING_ID = "valueAdvertisingId"

        private const val COLUMN_INDEX_DEVICE_ID = 1
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 2
        private const val COLUMN_INDEX_PUSH_TOKEN = 3
        private const val COLUMN_INDEX_CATEGORY = 4
        private const val COLUMN_INDEX_OS_TYPE = 5
        private const val COLUMN_INDEX_OS_VERSION = 6
        private const val COLUMN_INDEX_DEVICE_MODEL = 7
        private const val COLUMN_INDEX_APP_VERSION = 8
        private const val COLUMN_INDEX_LANGUAGE_CODE = 9
        private const val COLUMN_INDEX_TIME_ZONE = 10
        private const val COLUMN_INDEX_ADVERTISING_ID = 11
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
        val device = Device(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            pushToken = PUSH_TOKEN,
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
            DbSchema.DeviceSchema.COLUMN_DEVICE_ID,
            DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID,
            DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN,
            DbSchema.DeviceSchema.COLUMN_CATEGORY,
            DbSchema.DeviceSchema.COLUMN_OS_TYPE,
            DbSchema.DeviceSchema.COLUMN_OS_VERSION,
            DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL,
            DbSchema.DeviceSchema.COLUMN_APP_VERSION,
            DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE,
            DbSchema.DeviceSchema.COLUMN_TIMEZONE,
            DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID
        )

        // When
        contentValues.putDevice(device)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(DbSchema.DeviceSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID))
        assertEquals(PUSH_TOKEN, contentValues.get(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN))
        assertEquals(CATEGORY.toString(), contentValues.get(DbSchema.DeviceSchema.COLUMN_CATEGORY))
        assertEquals(OS_TYPE.toString(), contentValues.get(DbSchema.DeviceSchema.COLUMN_OS_TYPE))
        assertEquals(OS_VERSION, contentValues.get(DbSchema.DeviceSchema.COLUMN_OS_VERSION))
        assertEquals(DEVICE_MODEL, contentValues.get(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL))
        assertEquals(APP_VERSION, contentValues.get(DbSchema.DeviceSchema.COLUMN_APP_VERSION))
        assertEquals(LANGUAGE_CODE, contentValues.get(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(DbSchema.DeviceSchema.COLUMN_TIMEZONE))
        assertEquals(ADVERTISING_ID, contentValues.get(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID))
    }

    @Test
    fun givenCursorWithDeviceProvided_whenGetDevice_thenDeviceReturned() {
        // Given
        mockDeviceFull()

        val expectedDevice = Device(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            pushToken = PUSH_TOKEN,
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
            category = Device.fetchDeviceCategory(),
            osType = DeviceOS.ANDROID,
            osVersion = Device.fetchOsVersion(),
            deviceModel = Device.fetchDeviceModel(),
            appVersion = Device.fetchAppVersion(),
            languageCode = Device.fetchLanguageCode(),
            timeZone = Device.fetchTimeZone(),
            advertisingId = null
        )

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
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN) } returns COLUMN_INDEX_PUSH_TOKEN
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_CATEGORY) } returns COLUMN_INDEX_CATEGORY
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_TYPE) } returns COLUMN_INDEX_OS_TYPE
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_VERSION) } returns COLUMN_INDEX_OS_VERSION
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL) } returns COLUMN_INDEX_DEVICE_MODEL
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_APP_VERSION) } returns COLUMN_INDEX_APP_VERSION
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_TIMEZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID) } returns COLUMN_INDEX_ADVERTISING_ID
    }
    // endregion helper methods --------------------------------------------------------------------
}