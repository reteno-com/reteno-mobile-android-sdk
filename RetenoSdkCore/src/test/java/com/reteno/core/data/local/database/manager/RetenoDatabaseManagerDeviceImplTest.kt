package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.database.util.getDevice
import com.reteno.core.data.local.database.util.putDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.util.Logger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test


class RetenoDatabaseManagerDeviceImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val TIMESTAMP = "TimeStampHere_Z"

        private const val ROW_ID_1 = "12"
        private const val ROW_ID_2 = "13"
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
        private const val EMAIL = "valueEmail"
        private const val PHONE = "valuePhone"

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
        private const val COLUMN_INDEX_EMAIL = 14
        private const val COLUMN_INDEX_PHONE = 15


        private val device1 = DeviceDb(
            rowId = ROW_ID_1,
            deviceId = DEVICE_ID,
            externalUserId = null,
            pushToken = PUSH_TOKEN,
            pushSubscribed = PUSH_SUBSCRIBED,
            category = CATEGORY,
            osType = OS_TYPE,
            osVersion = null,
            deviceModel = null,
            appVersion = null,
            languageCode = null,
            timeZone = null,
            advertisingId = null,
            email = null,
            phone = null
        )

        private val device2 = DeviceDb(
            rowId = ROW_ID_2,
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
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor

    private lateinit var SUT: RetenoDatabaseManagerDevice
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_TIMESTAMP) } returns TIMESTAMP
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerDeviceImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
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
        SUT.insertDevice(device2)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
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
        val devices = SUT.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(2, devices.size)
        assertEquals(device1, devices[0])
        assertEquals(device2, devices[1])
    }

    @Test
    fun givenDeviceNotAvailableInDatabase_whenGetDevice_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        // When
        val devices = SUT.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices.isEmpty())
    }

    @Test
    fun givenDeviceCorruptedInDatabaseAndRowIdDetected_whenGetDevice_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()

        every { cursor.getDevice() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_DEVICE_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val devices = SUT.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) {
            database.delete(
                DeviceSchema.TABLE_NAME_DEVICE,
                "${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?",
                arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices.isEmpty())
    }

    @Test
    fun givenDeviceCorruptedInDatabaseAndRowIdNotDetected_whenGetDevice_thenExceptionIsLogged() = runTest {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()

        every { cursor.getDevice() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_DEVICE_ROW_ID) } returns null

        // When
        val devices = SUT.getDevices(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
                columns = eq(DeviceSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(devices.isEmpty())
    }

    @Test
    fun givenDeviceCountEmpty_whenGetDeviceCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(DeviceSchema.TABLE_NAME_DEVICE) } returns recordsCount

        // When
        val count = SUT.getUnSyncedDeviceCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenDeviceCountNonEmpty_whenGetDeviceCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every {
            database.getRowCount(
                DeviceSchema.TABLE_NAME_DEVICE,
                whereClause = "${DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND}<>?",
                whereArgs = arrayOf(BooleanDb.TRUE.toString())
            )
        } returns recordsCount

        // When
        val count = SUT.getUnSyncedDeviceCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenDeviceProvided_wheDeleteDevice_thenDeleteFromDatabaseCalled() {
        // When
        SUT.deleteDevice(device1)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(DeviceSchema.TABLE_NAME_DEVICE),
                whereClause = eq("${DeviceSchema.COLUMN_DEVICE_ROW_ID}=?"),
                whereArgs = arrayOf(ROW_ID_1)
            )
        }
    }

    @Test
    fun givenDatabaseDeleteReturns1_wheDeleteDevice_thenResultIsTrue() {
        // Given
        every { database.delete(table = any(), whereClause = any(), whereArgs = any()) } returns 1

        // When
        val result = SUT.deleteDevice(device1)

        // Then
        assertTrue(result)
    }

    @Test
    fun givenDatabaseDeleteReturns0_wheDeleteDevice_thenResultIsFalse() {
        // Given
        every { database.delete(table = any(), whereClause = any(), whereArgs = any()) } returns 0

        // When
        val result = SUT.deleteDevice(device1)

        // Then
        assertFalse(result)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP) } returns COLUMN_INDEX_TIMESTAMP
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_ROW_ID) } returns COLUMN_INDEX_DEVICE_ROW_ID

        every { cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_PUSH_TOKEN) } returns COLUMN_INDEX_PUSH_TOKEN
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_CATEGORY) } returns COLUMN_INDEX_CATEGORY
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_OS_TYPE) } returns COLUMN_INDEX_OS_TYPE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_OS_VERSION) } returns COLUMN_INDEX_OS_VERSION
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_DEVICE_MODEL) } returns COLUMN_INDEX_DEVICE_MODEL
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_APP_VERSION) } returns COLUMN_INDEX_APP_VERSION
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_TIMEZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_ADVERTISING_ID) } returns COLUMN_INDEX_ADVERTISING_ID
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_EMAIL) } returns COLUMN_INDEX_EMAIL
        every { cursor.getColumnIndex(DeviceSchema.COLUMN_PHONE) } returns COLUMN_INDEX_PHONE

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
                table = DeviceSchema.TABLE_NAME_DEVICE,
                columns = DeviceSchema.getAllColumns(),
                orderBy = any(),
                limit = null
            )
        } returns cursor
    }
    // endregion helper methods --------------------------------------------------------------------
}