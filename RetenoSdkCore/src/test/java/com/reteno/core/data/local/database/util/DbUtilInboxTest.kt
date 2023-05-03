package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test


class DbUtilInboxTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val INBOX_ID = "214-asf-42412-dgjh-24512-mcgsd"
        private val INBOX_STATUS = AppInboxMessageStatusDb.OPENED
        private const val INBOX_OCCURRED_TIME = "2022-11-22T13:38:01Z"
        private const val INBOX_DEVICE_ID = "device_test"

        private const val COLUMN_APP_INBOX_ID = 1
        private const val COLUMN_APP_INBOX_DEVICE_ID = 2
        private const val COLUMN_APP_INBOX_STATUS = 3
        private const val COLUMN_APP_INBOX_TIME = 4
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
    fun givenInboxProvided_whenPutInbox_thenContentValuesUpdated() {
        // Given
        val messages = AppInboxMessageDb(
            id = INBOX_ID,
            status = INBOX_STATUS,
            occurredDate = INBOX_OCCURRED_TIME,
            deviceId = INBOX_DEVICE_ID
        )

        val keySet = arrayOf(
            AppInboxSchema.COLUMN_APP_INBOX_ID,
            AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID,
            AppInboxSchema.COLUMN_APP_INBOX_STATUS,
            AppInboxSchema.COLUMN_APP_INBOX_TIME
        )

        // When
        contentValues.putAppInbox(messages)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())
        assertEquals(INBOX_ID, contentValues.get(AppInboxSchema.COLUMN_APP_INBOX_ID))
        assertEquals(INBOX_STATUS.toString(), contentValues.get(AppInboxSchema.COLUMN_APP_INBOX_STATUS))
        assertEquals(INBOX_OCCURRED_TIME, contentValues.get(AppInboxSchema.COLUMN_APP_INBOX_TIME))
        assertEquals(INBOX_DEVICE_ID, contentValues.get(AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID))
    }

    @Test
    fun givenInboxFull_whenGetInbox_thenInboxReturned() {
        // Given
        mockInboxFull()

        val messages = AppInboxMessageDb(
            id = INBOX_ID,
            status = INBOX_STATUS,
            occurredDate = INBOX_OCCURRED_TIME,
            deviceId = INBOX_DEVICE_ID
        )

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertEquals(messages, actualInbox)
    }

    @Test
    fun givenInboxEmpty_whenGetInbox_thenNullReturned() {
        // Given
        mockInboxEmpty()

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertNull(actualInbox)
    }

    @Test
    fun givenInboxIdNull_whenGetInbox_thenNullReturned() {
        // Given
        mockInboxIdNull()

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertNull(actualInbox)
    }

    @Test
    fun givenInboxStatusNull_whenGetInbox_thenNullReturned() {
        // Given
        mockInboxStatusNull()

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertNull(actualInbox)
    }

    @Test
    fun givenInboxTimeNull_whenGetInbox_thenNullReturned() {
        // Given
        mockInboxTimeNull()

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertNull(actualInbox)
    }

    @Test
    fun givenInboxTokenNull_whenGetInbox_thenNullReturned() {
        // Given
        mockInboxTokenNull()

        // When
        val actualInbox = cursor.getAppInbox()

        // Then
        assertNull(actualInbox)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockInboxFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns INBOX_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns INBOX_DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns INBOX_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns INBOX_OCCURRED_TIME
    }

    private fun mockInboxEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns null
    }

    private fun mockInboxIdNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns INBOX_DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns INBOX_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns INBOX_OCCURRED_TIME
    }

    private fun mockInboxStatusNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns INBOX_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns INBOX_DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns INBOX_OCCURRED_TIME
    }

    private fun mockInboxTimeNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns INBOX_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns INBOX_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns INBOX_OCCURRED_TIME
    }

    private fun mockInboxTokenNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_APP_INBOX_ID) } returns INBOX_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_DEVICE_ID) } returns INBOX_DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_STATUS) } returns INBOX_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_APP_INBOX_TIME) } returns null
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_ID) } returns COLUMN_APP_INBOX_ID
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID) } returns COLUMN_APP_INBOX_DEVICE_ID
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_STATUS) } returns COLUMN_APP_INBOX_STATUS
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_TIME) } returns COLUMN_APP_INBOX_TIME
    }
    // endregion helper methods --------------------------------------------------------------------
}