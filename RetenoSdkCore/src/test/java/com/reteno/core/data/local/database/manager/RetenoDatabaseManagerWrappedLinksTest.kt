package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.WrappedLinkSchema
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerWrappedLinksTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val ROW_ID = 222L
        private const val TIMESTAMP = "TimeStampHere_Z"

        private const val URL_1 = "https://url.1.com"
        private const val URL_2 = "https://url.2.com"

        private const val COLUMN_INDEX_ROW_ID = 1
        private const val COLUMN_INDEX_TIMESTAMP = 2
        private const val COLUMN_INDEX_URL = 3
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor

    private lateinit var SUT: RetenoDatabaseManagerWrappedLinksImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_TIMESTAMP) } returns TIMESTAMP
        every { cursor.getLongOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerWrappedLinksImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
    }

    @Test
    fun whenInsertWrapped_thenWrappedLinkInsertedIntoDatabase() {
        // Given
        val expectedContentValues = ContentValues().apply {
            put(WrappedLinkSchema.COLUMN_URL, URL_1)
        }

        var actualContentValues = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT.insertWrappedLink(URL_1)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                contentValues = any()
            )
        }
        Assert.assertEquals(expectedContentValues, actualContentValues)
    }

    @Test
    fun givenWrappedLinksAvailableInDatabase_whenGetWrappedLinks_thenWrappedLinksReturned() {
        // Given
        mockCursorRecordsNumber(2)
        mockDatabaseQuery()

        every { cursor.getStringOrNull(COLUMN_INDEX_URL) } returns URL_1 andThen URL_2

        // When
        val wrappedLinks = SUT.getWrappedLinks()

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                columns = eq(WrappedLinkSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
            )
        }
        verify(exactly = 1) { cursor.close() }

        Assert.assertEquals(2, wrappedLinks.size)
        Assert.assertEquals(URL_1, wrappedLinks[0])
        Assert.assertEquals(URL_2, wrappedLinks[1])
    }

    @Test
    fun givenWrappedLinksNotAvailableInDatabase_whenGetWrappedLinks_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        // When
        val wrappedLinks = SUT.getWrappedLinks()

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                columns = eq(WrappedLinkSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(wrappedLinks.isEmpty())
    }

    @Test
    fun givenWrappedLinksCorruptedInDatabaseAndRowIdDetected_whenGetWrappedLinks_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getStringOrNull(COLUMN_INDEX_URL) } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val wrappedLinks = SUT.getWrappedLinks()

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                columns = eq(WrappedLinkSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                whereClause = eq("${WrappedLinkSchema.COLUMN_ROW_ID}=?"),
                whereArgs = eq(arrayOf(ROW_ID_CORRUPTED.toString()))
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(wrappedLinks.isEmpty())
    }

    @Test
    fun givenWrappedLinksCorruptedInDatabaseAndRowIdNotDetected_whenGetWrappedLinks_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getStringOrNull(COLUMN_INDEX_URL) } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_ROW_ID) } returns null

        // When
        val wrappedLinks = SUT.getWrappedLinks()

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                columns = eq(WrappedLinkSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(wrappedLinks.isEmpty())
    }

    @Test
    fun givenWrappedLinksCountEmpty_whenGetWrappedLinksCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK) } returns recordsCount

        // When
        val count = SUT.getWrappedLinksCount()

        // Then
        Assert.assertEquals(recordsCount, count)
    }

    @Test
    fun givenWrappedLinksCountNonEmpty_whenGetWrappedLinksCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK) } returns recordsCount

        // When
        val count = SUT.getWrappedLinksCount()

        // Then
        Assert.assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteWrappedLinksOldest_thenWrappedLinksDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${WrappedLinkSchema.COLUMN_ROW_ID} " +
                "in (select ${WrappedLinkSchema.COLUMN_ROW_ID} " +
                "from ${WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK} " +
                "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteWrappedLinks(count, true)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                whereClause = eq(whereClauseExpected)
            )
        }
    }

    @Test
    fun given_whenDeleteWrappedLinksNewest_thenWrappedLinksDeleted() {
        // Given
        val order = "DESC"
        val count = 4
        val whereClauseExpected = "${WrappedLinkSchema.COLUMN_ROW_ID} " +
                "in (select ${WrappedLinkSchema.COLUMN_ROW_ID} " +
                "from ${WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK} " +
                "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteWrappedLinks(count, false)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                whereClause = eq(whereClauseExpected)
            )
        }
    }

    @Test
    fun whenDeleteWrappedLinksByTime_thenWrappedLinksDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val countExpected = 2
        val whereClauseExpected = "${DbSchema.COLUMN_TIMESTAMP} < '$outdatedTime'"

        every { database.delete(any(), any(), any()) } returns countExpected

        // When
        SUT.deleteWrappedLinksByTime(outdatedTime)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK),
                whereClause = eq(whereClauseExpected)
            )
        }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getColumnIndex(WrappedLinkSchema.COLUMN_ROW_ID) } returns COLUMN_INDEX_ROW_ID
        every { cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP) } returns COLUMN_INDEX_TIMESTAMP
        every { cursor.getColumnIndex(WrappedLinkSchema.COLUMN_URL) } returns COLUMN_INDEX_URL
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
                table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
                columns = WrappedLinkSchema.getAllColumns(),
                orderBy = any(),
                limit = any()
            )
        } returns cursor
    }
    // endregion helper methods --------------------------------------------------------------------
}