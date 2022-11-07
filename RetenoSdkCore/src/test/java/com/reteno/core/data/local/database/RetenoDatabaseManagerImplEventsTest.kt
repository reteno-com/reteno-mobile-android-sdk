package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.event.EventDTO
import com.reteno.core.data.remote.model.event.EventsDTO
import com.reteno.core.data.remote.model.event.ParameterDTO
import com.reteno.core.util.Logger
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.*
import net.sqlcipher.Cursor
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerImplEventsTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val PARENT_ROW_ID_NOT_FOUND = -1L

        private const val EVENT_TYPE_KEY_1 = "eventTypeKey_1"
        private val EVENT_OCCURRED_1 = ZonedDateTime.now().minusDays(1).toString()
        private const val EVENT_TYPE_KEY_2 = "eventTypeKey_2"
        private val EVENT_OCCURRED_2 = ZonedDateTime.now().toString()
        private const val EVENT_PARAMS_NAME_1 = "params_name1"
        private const val EVENT_PARAMS_VALUE_1 = "params_value1"
        private const val EVENT_PARAMS_NAME_2 = "params_name2"
        private const val EVENT_PARAMS_VALUE_2 = "params_value2"

        private const val DEVICE_ID = "valueDeviceId"
        private const val EXTERNAL_USER_ID = "valueExternalUserId"

        private const val COLUMN_INDEX_EVENTS_ID = 1
        private const val COLUMN_INDEX_EVENTS_DEVICE_ID = 2
        private const val COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID = 3
        private const val COLUMN_INDEX_EVENT_ROW_ID = 4
        private const val COLUMN_INDEX_EVENT_TYPE_KEY = 5
        private const val COLUMN_INDEX_EVENT_OCCURRED = 6
        private const val COLUMN_INDEX_EVENT_PARAMS = 7
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor
    @MockK
    private lateinit var cursorChild: Cursor
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT: RetenoDatabaseManagerImpl? = null

    override fun before() {
        super.before()
        mockkStatic(Cursor::getEvent)

        mockColumnIndexes()
        justRun { cursor.close() }
        justRun { cursorChild.close() }

        SUT = RetenoDatabaseManagerImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
        clearMocks(cursorChild)
        unmockkStatic(Cursor::getEvent)
    }

    @Test
    fun givenValidEventsProvidedEventsNotPresentInDatabase_whenInsertEvents_thenEventsIsSavedToDb() {
        // Given
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns PARENT_ROW_ID_NOT_FOUND

        val param1 = ParameterDTO(name = EVENT_PARAMS_NAME_1, value = EVENT_PARAMS_VALUE_1)
        val param2 = ParameterDTO(name = EVENT_PARAMS_NAME_2, value = EVENT_PARAMS_VALUE_2)
        val event1 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_1, occurred = EVENT_OCCURRED_1, params = null)
        val event2 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_2, occurred = EVENT_OCCURRED_2, params = listOf(param1, param2))
        val events = EventsDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = listOf(event1, event2)
        )

        val expectedParentContentValues = ContentValues().apply {
            putEvents(events)
        }
        var actualParentContentValues = ContentValues()
        every { database.insert(DbSchema.EventsSchema.TABLE_NAME_EVENTS, null, any()) } answers {
            actualParentContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }
        val expectedChildContentValues = events.eventList.toContentValuesList(ROW_ID_INSERTED)

        // When
        SUT?.insertEvents(events)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DbSchema.EventsSchema.TABLE_NAME_EVENTS),
                contentValues = any()
            )
        }
        assertEquals(expectedParentContentValues, actualParentContentValues)

        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(DbSchema.EventSchema.TABLE_NAME_EVENT),
                contentValues = eq(expectedChildContentValues)
            )
        }
    }

    @Test
    fun givenValidEventsProvidedEventsPresentInDatabase_whenInsertEvents_thenEventsIsSavedToDb() {
        // Given
        mockDatabaseQueryParent()
        every { cursor.moveToFirst() } returns true
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED

        val param1 = ParameterDTO(name = EVENT_PARAMS_NAME_1, value = EVENT_PARAMS_VALUE_1)
        val param2 = ParameterDTO(name = EVENT_PARAMS_NAME_2, value = EVENT_PARAMS_VALUE_2)
        val event1 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_1, occurred = EVENT_OCCURRED_1, params = null)
        val event2 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_2, occurred = EVENT_OCCURRED_2, params = listOf(param1, param2))
        val events = EventsDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = listOf(event1, event2)
        )

        val expectedChildContentValues = events.eventList.toContentValuesList(ROW_ID_INSERTED)

        // When
        SUT?.insertEvents(events)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns(),
                selection = "${DbSchema.EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${DbSchema.EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID}=?",
                selectionArgs = arrayOf(DEVICE_ID, EXTERNAL_USER_ID)
            )
        }
        verify(exactly = 0) {
            database.insert(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                contentValues = any()
            )
        }
        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(DbSchema.EventSchema.TABLE_NAME_EVENT),
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

        val param1 = ParameterDTO(name = EVENT_PARAMS_NAME_1, value = EVENT_PARAMS_VALUE_1)
        val param2 = ParameterDTO(name = EVENT_PARAMS_NAME_2, value = EVENT_PARAMS_VALUE_2)
        val event1 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_1, occurred = EVENT_OCCURRED_1, params = null)
        val event2 = EventDTO(eventTypeKey = EVENT_TYPE_KEY_2, occurred = EVENT_OCCURRED_2, params = listOf(param1, param2))

        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursorChild.getEvent() } returns event1 andThen event2

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventSchema.TABLE_NAME_EVENT,
                columns = DbSchema.EventSchema.getAllColumns(),
                selection = "${DbSchema.EventsSchema.COLUMN_EVENTS_ID}=?",
                selectionArgs = arrayOf(ROW_ID_INSERTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        assertEquals(1, actualEvents?.size)
        assertEquals(actualEvents?.get(0)?.eventList?.get(0), event1)
        assertEquals(actualEvents?.get(0)?.eventList?.get(1), event2)
    }

    @Test
    fun givenEventsParentNotAvailableInDatabase_whenGetEvents_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(cursor,0)
        mockCursorRecordsNumber(cursorChild,0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(DbSchema.EventsSchema.TABLE_NAME_EVENTS),
                columns = eq(DbSchema.EventsSchema.getAllColumns())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(listOf<EventDTO>(), actualEvents)
    }

    @Test
    fun givenEventsParentNotAvailableEventsChildAvailableInDatabase_whenGetEvents_thenEmptyListReturnedParentEventsRemoved() {
        // Given
        mockCursorRecordsNumber(cursor,1)
        mockCursorRecordsNumber(cursorChild,0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventSchema.TABLE_NAME_EVENT,
                columns = DbSchema.EventSchema.getAllColumns(),
                selection = "${DbSchema.EventsSchema.COLUMN_EVENTS_ID}=?",
                selectionArgs = arrayOf(ROW_ID_INSERTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        verify(exactly = 1) { database.cleanUnlinkedEvents() }

        assertEquals(listOf<EventDTO>(), actualEvents)
    }

    @Test
    fun givenEventsCorruptedInDatabaseAndRowIdDetected_whenGetEvents_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getEvent() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_CORRUPTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                whereClause = "${DbSchema.EventsSchema.COLUMN_EVENTS_ID}=?",
                whereArgs = arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents?.isEmpty() ?: false)
    }

    @Test
    fun givenEventsCorruptedInDatabaseAndRowIdNotDetected_whenGetEvents_thenExceptionIsLogged() {
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getEvent() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 0) {
            database.delete(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                whereClause = any(),
                whereArgs = any()
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents?.isEmpty() ?: false)
    }

    @Test
    fun givenEventsChildCorruptedInDatabaseAndRowIdDetected_whenGetEvents_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 1)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getEvent() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursorChild.getEvent() } returns null
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = DbSchema.EventSchema.TABLE_NAME_EVENT,
                whereClause = "${DbSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }
        assertTrue(actualEvents?.isEmpty() ?: false)
    }

    @Test
    fun givenEventsChildCorruptedInDatabaseAndRowIdNotDetected_whenGetEvents_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 1)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getEvent() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursorChild.getEvent() } returns null
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns null

        // When
        val actualEvents = SUT?.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 0) {
            database.delete(
                table = DbSchema.EventSchema.TABLE_NAME_EVENT,
                whereClause = any(),
                whereArgs = any()
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }
        assertTrue(actualEvents?.isEmpty() ?: false)
    }

    @Test
    fun givenEventsCountEmpty_whenGetEventsCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns recordsCount

        // When
        val count = SUT?.getEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenEventsCountNonEmpty_whenGetEventsCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns recordsCount

        // When
        val count = SUT?.getEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteEventsOldest_thenEventsDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${DbSchema.EventSchema.COLUMN_EVENT_ROW_ID} " +
                    "in (select ${DbSchema.EventSchema.COLUMN_EVENT_ROW_ID} " +
                    "from ${DbSchema.EventSchema.TABLE_NAME_EVENT} " +
                    "ORDER BY ${DbSchema.EventSchema.COLUMN_EVENT_OCCURRED} $order " +
                    "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteEvents(count, true)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.EventSchema.TABLE_NAME_EVENT, whereClauseExpected) }
    }

    @Test
    fun given_whenDeleteEventsNewest_thenEventsDeleted() {
        // Given
        val order = "DESC"
        val count = 5
        val whereClauseExpected = "${DbSchema.EventSchema.COLUMN_EVENT_ROW_ID} " +
                "in (select ${DbSchema.EventSchema.COLUMN_EVENT_ROW_ID} " +
                "from ${DbSchema.EventSchema.TABLE_NAME_EVENT} " +
                "ORDER BY ${DbSchema.EventSchema.COLUMN_EVENT_OCCURRED} $order " +
                "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteEvents(count, false)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.EventSchema.TABLE_NAME_EVENT, whereClauseExpected) }
    }


    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false
        every { cursorChild.isNull(any()) } returns false

        every { cursor.getColumnIndex(DbSchema.EventsSchema.COLUMN_EVENTS_ID) } returns COLUMN_INDEX_EVENTS_ID
        every { cursor.getColumnIndex(DbSchema.EventsSchema.COLUMN_EVENTS_DEVICE_ID) } returns COLUMN_INDEX_EVENTS_DEVICE_ID
        every { cursor.getColumnIndex(DbSchema.EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_ROW_ID) } returns COLUMN_INDEX_EVENT_ROW_ID
        every { cursor.getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_TYPE_KEY) } returns COLUMN_INDEX_EVENT_TYPE_KEY
        every { cursor.getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_OCCURRED) } returns COLUMN_INDEX_EVENT_OCCURRED
        every { cursor.getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_PARAMS) } returns COLUMN_INDEX_EVENT_PARAMS
        every { cursorChild.getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_ROW_ID) } returns COLUMN_INDEX_EVENT_ROW_ID
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
                table = DbSchema.EventsSchema.TABLE_NAME_EVENTS,
                columns = DbSchema.EventsSchema.getAllColumns(),
                selection = any(),
                selectionArgs = any(),
                groupBy = any(),
                having = any(),
                orderBy = any(),
                limit = any()
            )
        } returns cursor

        justRun { database.cleanUnlinkedEvents() }
    }

    private fun mockDatabaseQueryChild() {
        every {
            database.query(
                table = DbSchema.EventSchema.TABLE_NAME_EVENT,
                columns = DbSchema.EventSchema.getAllColumns(),
                selection = any(),
                selectionArgs = any(),
                groupBy = any(),
                having = any(),
                orderBy = any(),
                limit = any()
            )
        } returns cursorChild

        justRun { database.cleanUnlinkedEvents() }
    }
    // endregion helper methods --------------------------------------------------------------------
}