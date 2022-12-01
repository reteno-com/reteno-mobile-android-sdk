package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.database.util.getEvent
import com.reteno.core.data.local.database.util.getRecomEvent
import com.reteno.core.data.local.database.util.putRecomVariantId
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerRecomEventsTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L
        private const val ROW_ID_INSERTED = 1L

        private const val PRODUCT_ID_1 = "w12345s1345"
        private val RECOM_EVENT_TYPE_1 = RecomEventTypeDb.CLICKS
        private val RECOM_EVENT_OCCURRED_1 = ZonedDateTime.now().minusDays(1)
        private val RECOM_EVENT_1 = RecomEventDb(
            recomEventType = RECOM_EVENT_TYPE_1,
            occurred = RECOM_EVENT_OCCURRED_1.formatToRemote(),
            productId = PRODUCT_ID_1
        )

        private const val PRODUCT_ID_2 = "w999s999"
        private val RECOM_EVENT_TYPE_2 = RecomEventTypeDb.IMPRESSIONS
        private val RECOM_EVENT_OCCURRED_2 = ZonedDateTime.now().minusWeeks(1)
        private val RECOM_EVENT_2 = RecomEventDb(
            recomEventType = RECOM_EVENT_TYPE_2,
            occurred = RECOM_EVENT_OCCURRED_2.formatToRemote(),
            productId = PRODUCT_ID_2
        )

        private const val RECOM_VARIANT_ID = "r111v333"
        private val RECOM_EVENTS_LIST = listOf(RECOM_EVENT_1, RECOM_EVENT_2)

        private const val COLUMN_INDEX_RECOM_VARIANT_ID = 1
        private const val COLUMN_INDEX_RECOM_EVENT_ROW_ID = 2
        private const val COLUMN_INDEX_RECOM_EVENT_PRODUCT_ID = 3
        private const val COLUMN_INDEX_RECOM_EVENT_OCCURRED = 4
        private const val COLUMN_INDEX_RECOM_EVENT_TYPE = 5
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor
    @MockK
    private lateinit var cursorChild: Cursor

    private lateinit var SUT: RetenoDatabaseManagerRecomEvents
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkStatic(Cursor::getRecomEvent)

        mockColumnIndexes()
        justRun { cursor.close() }
        justRun { cursorChild.close() }

        SUT = RetenoDatabaseManagerRecomEventsImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
        clearMocks(cursorChild)
        unmockkStatic(Cursor::getEvent)
    }

    @Test
    fun givenValidEventsProvidedRecomVariantIdNotPresentInDatabase_whenInsertEvents_thenEventsIsSavedToDb() {
        // Given
        every { cursor.getLongOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns null

        val expectedParentContentValues = ContentValues().apply {
            putRecomVariantId(RECOM_VARIANT_ID)
        }
        var actualParentContentValues = ContentValues()
        every { database.insert(RecomEventsSchema.TABLE_NAME_RECOM_EVENTS, null, any()) } answers {
            actualParentContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }
        val expectedChildContentValues = RECOM_EVENTS_LIST.toContentValuesList(RECOM_VARIANT_ID)

        // When
        SUT.insertRecomEvents(RecomEventsDb(RECOM_VARIANT_ID, RECOM_EVENTS_LIST))

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(RecomEventsSchema.TABLE_NAME_RECOM_EVENTS),
                contentValues = any()
            )
        }
        assertEquals(expectedParentContentValues, actualParentContentValues)

        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT),
                contentValues = eq(expectedChildContentValues)
            )
        }
    }

    @Test
    fun givenValidRecomEventsProvidedRecomVariantIdPresentInDatabase_whenInsertRecomEvents_thenRecomEventsIsSavedToDb() {
        // Given
        mockDatabaseQueryParent()
        every { cursor.moveToFirst() } returns true
        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID

        val expectedChildContentValues = RECOM_EVENTS_LIST.toContentValuesList(RECOM_VARIANT_ID)

        // When
        SUT.insertRecomEvents(RecomEventsDb(RECOM_VARIANT_ID, RECOM_EVENTS_LIST))

        // Then
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns(),
                selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                selectionArgs = arrayOf(RECOM_VARIANT_ID)
            )
        }
        verify(exactly = 0) {
            database.insert(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                contentValues = any()
            )
        }
        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT),
                contentValues = eq(expectedChildContentValues)
            )
        }
    }

    @Test
    fun givenEventsAvailableInDatabase_whenGetEvents_thenEventsReturned() {
        // Given
        mockCursorRecordsNumber(cursor,1)
        mockCursorRecordsNumber(cursorChild,2)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursorChild.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursorChild.getRecomEvent() } returns RECOM_EVENT_1 andThen RECOM_EVENT_2

        // When
        val actualEvents = SUT.getRecomEvents()

        // Then
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                columns = RecomEventsSchema.RecomEventSchema.getAllColumns(),
                selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                selectionArgs = arrayOf(RECOM_VARIANT_ID)
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        assertEquals(1, actualEvents.size)
        assertEquals(actualEvents[0].recomEvents?.get(0), RECOM_EVENT_1)
        assertEquals(actualEvents[0].recomEvents?.get(1), RECOM_EVENT_2)
    }

    @Test
    fun givenRecomEventsParentNotAvailableInDatabase_whenGetRecomEvents_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(cursor,0)
        mockCursorRecordsNumber(cursorChild,0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        // When
        val actualEvents = SUT.getRecomEvents()

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(RecomEventsSchema.TABLE_NAME_RECOM_EVENTS),
                columns = eq(RecomEventsSchema.getAllColumns())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(listOf<RecomEventsDb>(), actualEvents)
    }

    @Test
    fun givenRecomEventsParentAvailableEventsChildNotAvailableInDatabase_whenGetRecomEvents_thenEmptyListReturnedCleanUnlinkedRecomVariantIdsCalled() {
        // Given
        mockCursorRecordsNumber(cursor,1)
        mockCursorRecordsNumber(cursorChild,0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursorChild.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns null
        every { cursorChild.getRecomEvent() } returns null

        // When
        val actualEvents = SUT.getRecomEvents()

        // Then
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                columns = RecomEventsSchema.RecomEventSchema.getAllColumns(),
                selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                selectionArgs = arrayOf(RECOM_VARIANT_ID)
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        verify(exactly = 1) { database.cleanUnlinkedRecomVariantIds() }

        assertEquals(listOf<RecomEventsDb>(), actualEvents)
    }

    @Test
    fun givenRecomEventCorruptedInDatabaseAndRowIdDetected_whenGetRecomEvents_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 1)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_RECOM_EVENT_ROW_ID) } returns ROW_ID_CORRUPTED
        every { cursorChild.getRecomEvent() } returns null

        // When
        val actualEvents = SUT.getRecomEvents()

        // Then
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                whereClause = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents.isEmpty())
    }

    @Test
    fun givenRecomEventCorruptedInDatabaseAndRowIdNotDetected_whenGetRecomEvents_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 1)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_RECOM_EVENT_ROW_ID) } returns null
        every { cursorChild.getRecomEvent() } returns null

        // When
        val actualEvents = SUT.getRecomEvents()

        // Then
        verify(exactly = 1) {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns()
            )
        }
        verify(exactly = 0) {
            database.delete(
                table = any(),
                whereClause = any(),
                whereArgs = any()
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }

        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents.isEmpty())
    }

    @Test
    fun givenRecomEventCountEmpty_whenGetRecomEventsCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT) } returns recordsCount

        // When
        val count = SUT.getRecomEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenRecomEventCountNonEmpty_whenGetRecomEventsCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT) } returns recordsCount

        // When
        val count = SUT.getRecomEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteRecomEventsOldest_thenRecomEventsDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID} " +
                    "in (select ${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID} " +
                    "from ${RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT} " +
                    "ORDER BY ${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED} $order " +
                    "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteRecomEvents(count, true)

        // Then
        verify(exactly = 1) { database.delete(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT, whereClauseExpected) }
    }

    @Test
    fun given_whenDeleteRecomEventsNewest_thenRecomEventsDeleted() {
        // Given
        val order = "DESC"
        val count = 5
        val whereClauseExpected = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID} " +
                "in (select ${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID} " +
                "from ${RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT} " +
                "ORDER BY ${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteRecomEvents(count, false)

        // Then
        verify(exactly = 1) { database.delete(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT, whereClauseExpected) }
    }

    @Test
    fun whenDeleteRecomEventsByTime_thenRecomEventsDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val countExpected = 2
        val whereClauseExpected = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED} < '$outdatedTime'"

        every { database.delete(any(), any(), any()) } returns countExpected

        // When
        SUT.deleteRecomEventsByTime(outdatedTime)

        // Then
        verify(exactly = 1) { database.delete(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT, whereClauseExpected) }
        verify(exactly = 1) { database.cleanUnlinkedRecomVariantIds() }
    }


    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false
        every { cursorChild.isNull(any()) } returns false

        every { cursor.getColumnIndex(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID) } returns COLUMN_INDEX_RECOM_VARIANT_ID

        every { cursorChild.getColumnIndex(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID) } returns COLUMN_INDEX_RECOM_VARIANT_ID
        every { cursorChild.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE) } returns COLUMN_INDEX_RECOM_EVENT_TYPE
        every { cursorChild.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID) } returns COLUMN_INDEX_RECOM_EVENT_PRODUCT_ID
        every { cursorChild.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED) } returns COLUMN_INDEX_RECOM_EVENT_OCCURRED
        every { cursorChild.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID) } returns COLUMN_INDEX_RECOM_EVENT_ROW_ID

    }

    private fun mockCursorRecordsNumber(cursor: Cursor, number: Int) {
        val responses = generateSequence(0) { it + 1 }
            .map { it < number }
            .take(number + 1)
            .toList()
        every { cursor.moveToNext() } returnsMany responses
    }

    private fun mockDatabaseQueryParent() {
        every {
            database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns(),
                selection = any(),
                selectionArgs = any(),
                groupBy = any(),
                having = any(),
                orderBy = any(),
                limit = any()
            )
        } returns cursor

        justRun { database.cleanUnlinkedRecomVariantIds() }
    }

    private fun mockDatabaseQueryChild() {
        every {
            database.query(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                columns = RecomEventsSchema.RecomEventSchema.getAllColumns(),
                selection = any(),
                selectionArgs = any(),
                groupBy = any(),
                having = any(),
                orderBy = any(),
                limit = any()
            )
        } returns cursorChild

        justRun { database.cleanUnlinkedRecomVariantIds() }
    }
    // endregion helper methods --------------------------------------------------------------------
}