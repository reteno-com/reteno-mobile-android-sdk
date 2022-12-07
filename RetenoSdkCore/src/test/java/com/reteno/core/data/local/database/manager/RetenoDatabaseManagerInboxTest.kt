package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.util.getAppInbox
import com.reteno.core.data.local.database.util.putAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerInboxTest : BaseRobolectricTest() {

    companion object {
        private const val ROW_ID_INSERTED = 1L

        private const val INBOX_ID = "214-asf-42412-dgjh-24512-mcgsd"
        private const val INBOX_ID_CORRUPTED = "214-asf-42412-dgjh-24512-mcgsd_c"
        private val INBOX_STATUS = AppInboxMessageStatusDb.OPENED
        private const val INBOX_OCCURRED_TIME = "2022-11-22T13:38:01Z"
        private const val INBOX_DEVICE_ID = "device_test"

        private val inbox1 = AppInboxMessageDb(
            id = INBOX_ID,
            status = INBOX_STATUS,
            occurredDate = INBOX_OCCURRED_TIME,
            deviceId = INBOX_DEVICE_ID
        )
        private val inbox2 = AppInboxMessageDb(
            id = "${INBOX_ID}_2",
            status = INBOX_STATUS,
            occurredDate = "${INBOX_OCCURRED_TIME}_2}",
            deviceId = INBOX_DEVICE_ID
        )

        private const val COLUMN_INDEX_INBOX_ID = 2
        private const val COLUMN_INDEX_INBOX_STATUS = 3
        private const val COLUMN_INDEX_INBOX_TIME = 4
        private const val COLUMN_INDEX_INBOX_DEVICE_ID = 5

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic(Cursor::getAppInbox)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(Cursor::getAppInbox)
        }
    }

    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor

    private lateinit var SUT: RetenoDatabaseManagerAppInbox

    override fun before() {
        super.before()

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_INBOX_TIME) } returns INBOX_OCCURRED_TIME
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerAppInboxImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
    }

    @Test
    fun givenValidInboxProvided_whenInsertInbox_thenInboxIsSavedToDb() {
        val expectedContentValues = ContentValues().apply {
            putAppInbox(inbox1)
        }

        var actualContentValues = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        SUT.insertAppInboxMessage(inbox1)

        verify(exactly = 1) {
            database.insert(
                table = eq(AppInboxSchema.TABLE_NAME_APP_INBOX),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValues, actualContentValues)
    }

    @Test
    fun givenInboxesAvailableInDatabase_whenGetAppInbox_thenInboxesReturned() {
        mockCursorRecordsNumber(2)
        mockDatabaseQuery()

        every { cursor.getAppInbox() } returns inbox1 andThen inbox2

        val interactions = SUT.getAppInboxMessages(null)

        verify(exactly = 1) {
            database.query(
                table = eq(AppInboxSchema.TABLE_NAME_APP_INBOX),
                columns = eq(AppInboxSchema.getAllColumns()),
                orderBy = eq("${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(2, interactions.size)
        assertEquals(inbox1, interactions[0])
        assertEquals(inbox2, interactions[1])
    }

    @Test
    fun givenInboxNotAvailableInDatabase_whenGetAppInbox_thenEmptyListReturned() {
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        val interactions = SUT.getAppInboxMessages(null)

        verify(exactly = 1) {
            database.query(
                table = eq(AppInboxSchema.TABLE_NAME_APP_INBOX),
                columns = eq(AppInboxSchema.getAllColumns()),
                orderBy = eq("${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInboxCorruptedInDatabaseAndRowIdDetected_whenGetAppInbox_thenCorruptedRowRemoved() {
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getAppInbox() } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INBOX_ID) } returns INBOX_ID_CORRUPTED

        val interactions = SUT.getAppInboxMessages(null)

        verify(exactly = 1) {
            database.query(
                table = eq(AppInboxSchema.TABLE_NAME_APP_INBOX),
                columns = eq(AppInboxSchema.getAllColumns()),
                orderBy = eq("${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) {
            database.delete(
                AppInboxSchema.TABLE_NAME_APP_INBOX,
                "${AppInboxSchema.COLUMN_APP_INBOX_ID}=?",
                arrayOf(INBOX_ID_CORRUPTED)
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInboxCorruptedInDatabaseAndRowIdNotDetected_whenGetAppInbox_thenExceptionIsLogged() {
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getAppInbox() } returns null

        val interactions = SUT.getAppInboxMessages(null)

        verify(exactly = 1) {
            database.query(
                table = eq(AppInboxSchema.TABLE_NAME_APP_INBOX),
                columns = eq(AppInboxSchema.getAllColumns()),
                orderBy = eq("${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInboxCountEmpty_whenGetAppInboxCount_thenZeroReturned() {
        val recordsCount = 0L
        every { database.getRowCount(AppInboxSchema.TABLE_NAME_APP_INBOX) } returns recordsCount

        val count = SUT.getAppInboxMessagesCount()

        assertEquals(recordsCount, count)
    }

    @Test
    fun givenInboxCountNonEmpty_whenGetAppInboxCount_thenCountReturned() {
        val recordsCount = 5L
        every { database.getRowCount(AppInboxSchema.TABLE_NAME_APP_INBOX) } returns recordsCount

        val count = SUT.getAppInboxMessagesCount()

        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteInboxesOldest_thenInboxesDeleted() {
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${AppInboxSchema.COLUMN_APP_INBOX_ID} " +
                "in (select ${AppInboxSchema.COLUMN_APP_INBOX_ID} " +
                "from ${AppInboxSchema.TABLE_NAME_APP_INBOX} " +
                "ORDER BY ${AppInboxSchema.COLUMN_APP_INBOX_TIME} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        SUT.deleteAppInboxMessages(count, true)

        verify(exactly = 1) {
            database.delete(
                AppInboxSchema.TABLE_NAME_APP_INBOX,
                whereClauseExpected
            )
        }
    }

    @Test
    fun given_whenDeleteInboxesNewest_thenInboxesDeleted() {
        val order = "DESC"
        val count = 4
        val whereClauseExpected = "${AppInboxSchema.COLUMN_APP_INBOX_ID} " +
                "in (select ${AppInboxSchema.COLUMN_APP_INBOX_ID} " +
                "from ${AppInboxSchema.TABLE_NAME_APP_INBOX} " +
                "ORDER BY ${AppInboxSchema.COLUMN_APP_INBOX_TIME} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        SUT.deleteAppInboxMessages(count, false)

        verify(exactly = 1) {
            database.delete(
                AppInboxSchema.TABLE_NAME_APP_INBOX,
                whereClauseExpected
            )
        }
    }

    @Test
    fun whenDeleteInboxesByTime_thenInboxesDeleted() {
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val countExpected = 2
        val whereClauseExpected =
            "${AppInboxSchema.COLUMN_APP_INBOX_TIME} < '$outdatedTime'"

        every { database.delete(any(), any(), any()) } returns countExpected

        SUT.deleteAppInboxMessagesByTime(outdatedTime)

        verify(exactly = 1) {
            database.delete(
                AppInboxSchema.TABLE_NAME_APP_INBOX,
                whereClauseExpected
            )
        }
    }

    @Test
    fun whenDeleteAllInboxes_thenInboxesDeleteAllInboxes_thenInboxesDeleted() {
        val countExpected = 2

        every { database.delete(any(), any(), any()) } returns countExpected

        SUT.deleteAllAppInboxMessages()

        verify(exactly = 1) {
            database.delete(AppInboxSchema.TABLE_NAME_APP_INBOX)
        }
    }


    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false
        
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_ID) } returns COLUMN_INDEX_INBOX_ID
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_STATUS) } returns COLUMN_INDEX_INBOX_STATUS
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_TIME) } returns COLUMN_INDEX_INBOX_TIME
        every { cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID) } returns COLUMN_INDEX_INBOX_DEVICE_ID
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
                table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                columns = AppInboxSchema.getAllColumns(),
                orderBy = any(),
                limit = null
            )
        } returns cursor
    }
}