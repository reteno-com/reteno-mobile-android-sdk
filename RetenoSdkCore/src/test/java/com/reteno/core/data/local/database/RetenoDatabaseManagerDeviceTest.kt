package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import com.reteno.core.util.Logger
import org.junit.Assert.assertEquals

import org.junit.Test

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor


class RetenoDatabaseManagerDeviceTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object Constants {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val TIMESTAMP = "TimeStampHere_Z"

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

        private const val COLUMN_INDEX_TIMESTAMP = 1
        private const val COLUMN_INDEX_DEVICE_ROW_ID = 2
        private const val COLUMN_INDEX_DEVICE_ID = 3
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 4
        private const val COLUMN_INDEX_PUSH_TOKEN = 5
        private const val COLUMN_INDEX_CATEGORY = 6
        private const val COLUMN_INDEX_OS_TYPE = 7
        private const val COLUMN_INDEX_OS_VERSION = 8
        private const val COLUMN_INDEX_DEVICE_MODEL = 9
        private const val COLUMN_INDEX_APP_VERSION = 10
        private const val COLUMN_INDEX_LANGUAGE_CODE = 11
        private const val COLUMN_INDEX_TIME_ZONE = 12
        private const val COLUMN_INDEX_ADVERTISING_ID = 13


        private val device1 = Device(
            deviceId = DEVICE_ID,
            externalUserId = null,
            pushToken = PUSH_TOKEN,
            category = CATEGORY,
            osType = OS_TYPE,
            osVersion = null,
            deviceModel = null,
            appVersion = null,
            languageCode = null,
            timeZone = null,
            advertisingId = null
        )

        private val device2 = Device(
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
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT: RetenoDatabaseManagerImpl? = null

    override fun before() {
        super.before()
        mockkStatic(Cursor::getDevice)

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_TIMESTAMP) } returns TIMESTAMP
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
        unmockkStatic(Cursor::getDevice)
    }

    @Test
    fun givenValidDeviceProvided_whenInsertDevice_thenDeviceIsSavedToDb() {
        // Given
        val expectedContentValues = ContentValues().apply {
            putDevice(device2)
        }

        var actualContentValues = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT?.insertDevice(device2)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DbSchema.DeviceSchema.TABLE_NAME_DEVICE),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValues, actualContentValues)
    }

    @Test
    fun givenDevicesAvailableInDatabase_whenGetDevice_thenDevicesReturned() {
        // Given
        mockCursorRecordsNumber(2)
        mockDatabaseQuery()

        every { cursor.getDevice() } returns device1 andThen device2

        // When
        val devices = SUT?.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DbSchema.DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DbSchema.DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(2, devices?.size)
        assertEquals(device1, devices?.get(0))
        assertEquals(device2, devices?.get(1))
    }

    @Test
    fun givenDeviceNotAvailableInDatabase_whenGetDevice_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        // When
        val devices = SUT?.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DbSchema.DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DbSchema.DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices?.isEmpty() ?: false)
    }

    @Test
    fun givenDeviceCorruptedInDatabaseAndRowIdDetected_whenGetDevice_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()

        every { cursor.getDevice() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_DEVICE_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val devices = SUT?.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DbSchema.DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DbSchema.DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) {
            database.delete(
                DbSchema.DeviceSchema.TABLE_NAME_DEVICE,
                "${DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
                arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices?.isEmpty() ?: false)
    }

    @Test
    fun givenDeviceCorruptedInDatabaseAndRowIdNotDetected_whenGetDevice_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()

        every { cursor.getDevice() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_DEVICE_ROW_ID) } returns null

        // When
        val devices = SUT?.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DbSchema.DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DbSchema.DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices?.isEmpty() ?: false)
    }

    @Test
    fun givenDeviceCountEmpty_whenGetDeviceCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns recordsCount

        // When
        val count = SUT?.getDeviceCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenDeviceCountNonEmpty_whenGetDeviceCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns recordsCount

        // When
        val count = SUT?.getDeviceCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteDevicesOldest_thenDevicesDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID} " +
                    "in (select ${DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID} " +
                    "from ${DbSchema.DeviceSchema.TABLE_NAME_DEVICE} " +
                    "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                    "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteDevices(count, true)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.DeviceSchema.TABLE_NAME_DEVICE, whereClauseExpected) }
    }

    @Test
    fun given_whenDeleteDevicesNewest_thenDevicesDeleted() {
        // Given
        val order = "DESC"
        val count = 4
        val whereClauseExpected = "${DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID} " +
                "in (select ${DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID} " +
                "from ${DbSchema.DeviceSchema.TABLE_NAME_DEVICE} " +
                "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteDevices(count, false)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.DeviceSchema.TABLE_NAME_DEVICE, whereClauseExpected) }
    }


    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP) } returns COLUMN_INDEX_TIMESTAMP
        every { cursor.getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_ROW_ID) } returns COLUMN_INDEX_DEVICE_ROW_ID

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

    private fun mockCursorRecordsNumber(number: Int) {
        val responses = generateSequence(0) { it + 1 }
            .map { it < number }
            .take(number + 1)
            .toList()
        every { cursor.moveToNext() } returnsMany responses
    }

    private fun mockDatabaseQuery() {
        every {
            database.query(
                table = DbSchema.DeviceSchema.TABLE_NAME_DEVICE,
                columns = DbSchema.DeviceSchema.getAllColumns(),
                orderBy = any(),
                limit = null
            )
        } returns cursor
    }
    // endregion helper methods --------------------------------------------------------------------
}