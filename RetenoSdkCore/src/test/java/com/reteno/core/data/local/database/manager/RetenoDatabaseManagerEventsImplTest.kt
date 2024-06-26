package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.database.util.getEvent
import com.reteno.core.data.local.database.util.putEvents
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicLong


class RetenoDatabaseManagerEventsImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val PARENT_ROW_ID_NOT_FOUND = -1L

        private const val EVENT_ROW_ID_1 = "12333"
        private const val EVENT_ROW_ID_2 = "1"
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

        private val param1 = ParameterDb(name = EVENT_PARAMS_NAME_1, value = EVENT_PARAMS_VALUE_1)
        private val param2 = ParameterDb(name = EVENT_PARAMS_NAME_2, value = EVENT_PARAMS_VALUE_2)
        private val event1 = EventDb(
            rowId = EVENT_ROW_ID_1,
            eventTypeKey = EVENT_TYPE_KEY_1,
            occurred = EVENT_OCCURRED_1,
            params = null
        )
        private val event2 = EventDb(
            rowId = EVENT_ROW_ID_2,
            eventTypeKey = EVENT_TYPE_KEY_2,
            occurred = EVENT_OCCURRED_2,
            params = listOf(
                param1, param2
            )
        )
        private val events = EventsDb(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = listOf(event1, event2)
        )

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

    private lateinit var SUT: RetenoDatabaseManagerEvents
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        mockColumnIndexes()
        justRun { cursor.close() }
        justRun { cursorChild.close() }

        SUT = RetenoDatabaseManagerEventsImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
        clearMocks(cursorChild)
    }

    @Test
    fun givenValidEventsProvidedEventsNotPresentInDatabase_whenInsertEvents_thenEventsIsSavedToDb() {
        // Given
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns PARENT_ROW_ID_NOT_FOUND

        val expectedParentContentValues = ContentValues().apply {
            putEvents(events)
        }
        var actualParentContentValues = ContentValues()
        every { database.insert(EventsSchema.TABLE_NAME_EVENTS, null, any()) } answers {
            actualParentContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }
        val expectedChildContentValues = events.eventList.toContentValuesList(ROW_ID_INSERTED)

        // When
        SUT.insertEvents(events)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(EventsSchema.TABLE_NAME_EVENTS),
                contentValues = any()
            )
        }
        assertEquals(expectedParentContentValues, actualParentContentValues)

        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                contentValues = eq(expectedChildContentValues)
            )
        }
    }

    @Test
    fun givenLotsOfValidEventsMultithreaded_whenInsertEvents_thenNoException() = runBlocking {
        // Given
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns PARENT_ROW_ID_NOT_FOUND

        val atomic = AtomicLong(0L)

        every { database.insert(EventsSchema.TABLE_NAME_EVENTS, null, any()) } answers {
            atomic.addAndGet(1)
        }
        val testDataSet = buildList {
            repeat(1000) {
                val event = EventsDb(
                    deviceId = DEVICE_ID,
                    externalUserId = EXTERNAL_USER_ID + it,
                    eventList = listOf(event1, event2)
                )
                add(event to ContentValues().apply { putEvents(event) })
            }
        }

        // When
        testDataSet.map {
            async(Dispatchers.IO) { SUT.insertEvents(it.first) }
        }.awaitAll()

        // Then
        testDataSet.forEach {
            verify {
                database.insert(
                    table = eq(EventsSchema.TABLE_NAME_EVENTS),
                    nullColumnHack = null,
                    contentValues = eq(it.second)
                )
            }
        }
    }

    @Test
    fun givenValidEventsProvidedEventsPresentInDatabase_whenInsertEvents_thenEventsIsSavedToDb() {
        // Given
        mockDatabaseQueryParent()
        every { cursor.moveToFirst() } returns true
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED

        val expectedChildContentValues = events.eventList.toContentValuesList(ROW_ID_INSERTED)

        // When
        SUT.insertEvents(events)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns(),
                selection = "${EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID}=?",
                selectionArgs = arrayOf(DEVICE_ID, EXTERNAL_USER_ID)
            )
        }
        verify(exactly = 0) {
            database.insert(
                table = EventsSchema.TABLE_NAME_EVENTS,
                contentValues = any()
            )
        }
        verify(exactly = 1) {
            database.insertMultiple(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                contentValues = eq(expectedChildContentValues)
            )
        }
    }

    @Test
    fun givenEventsAvailableInDatabase_whenGetEvents_thenEventsReturned() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 2)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursorChild.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursorChild.getEvent() } returns event1 andThen event2

        // When
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                columns = EventsSchema.EventSchema.getAllColumns(),
                selection = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                selectionArgs = arrayOf(ROW_ID_INSERTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        assertEquals(1, actualEvents.size)
        assertEquals(actualEvents[0].eventList[0], event1)
        assertEquals(actualEvents[0].eventList[1], event2)
    }

    @Test
    fun givenEventsParentNotAvailableInDatabase_whenGetEvents_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(cursor, 0)
        mockCursorRecordsNumber(cursorChild, 0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        // When
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(EventsSchema.TABLE_NAME_EVENTS),
                columns = eq(EventsSchema.getAllColumns())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(listOf<EventDb>(), actualEvents)
    }

    @Test
    fun givenEventsParentNotAvailableEventsChildAvailableInDatabase_whenGetEvents_thenEmptyListReturnedParentEventsRemoved() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns ROW_ID_INSERTED
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID

        // When
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                columns = EventsSchema.EventSchema.getAllColumns(),
                selection = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                selectionArgs = arrayOf(ROW_ID_INSERTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }

        verify(exactly = 1) { database.cleanUnlinkedEvents() }

        assertEquals(listOf<EventDb>(), actualEvents)
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
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = EventsSchema.TABLE_NAME_EVENTS,
                whereClause = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                whereArgs = arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents?.isEmpty() ?: false)
    }

    @Test
    fun givenEventsCorruptedInDatabaseAndRowIdNotDetected_whenGetEvents_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(cursor, 1)
        mockCursorRecordsNumber(cursorChild, 0)
        mockDatabaseQueryParent()
        mockDatabaseQueryChild()

        every { cursor.getEvent() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_EVENTS_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID

        // When
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 0) {
            database.delete(
                table = EventsSchema.TABLE_NAME_EVENTS,
                whereClause = any(),
                whereArgs = any()
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }
        assertTrue(actualEvents.isEmpty())
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
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                whereClause = "${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }
        assertTrue(actualEvents.isEmpty())
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
        val actualEvents = SUT.getEvents(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
        }
        verify(exactly = 0) {
            database.delete(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                whereClause = any(),
                whereArgs = any()
            )
        }
        verify(exactly = 1) { cursor.close() }
        verify(exactly = 1) { cursorChild.close() }
        assertTrue(actualEvents.isEmpty())
    }

    @Test
    fun givenEventsCountEmpty_whenGetEventsCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(EventsSchema.EventSchema.TABLE_NAME_EVENT) } returns recordsCount

        // When
        val count = SUT.getEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenEventsCountNonEmpty_whenGetEventsCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(EventsSchema.EventSchema.TABLE_NAME_EVENT) } returns recordsCount

        // When
        val count = SUT.getEventsCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenEventsNotEmpty_wheDeleteEvents_thenDeleteFromDatabaseCalled() {
        // When
        SUT.deleteEvents(events)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                whereClause = eq("${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?"),
                whereArgs = eq(arrayOf(EVENT_ROW_ID_1))
            )
        }
        verify(exactly = 1) {
            database.delete(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                whereClause = eq("${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?"),
                whereArgs = eq(arrayOf(EVENT_ROW_ID_2))
            )
        }
    }

    @Test
    fun givenEventsEmpty_wheDeleteEvents_thenDeleteFromDatabaseNotCalled() {
        // Given
        val events = EventsDb(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = emptyList()
        )

        // When
        SUT.deleteEvents(events)

        // Then
        verify(exactly = 0) {
            database.delete(
                table = any(),
                whereClause = any(),
                whereArgs = any()
            )
        }
    }

    @Test
    fun givenEventsWithEmptyRowIds_wheDeleteEvents_thenDeleteFromDatabaseNotCalled() {
        // Given
        val events = EventsDb(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = listOf(event1.copy(rowId = null), event2.copy(rowId = null))
        )

        // When
        SUT.deleteEvents(events)

        // Then
        verify(exactly = 0) {
            database.delete(
                table = any(),
                whereClause = any(),
                whereArgs = any()
            )
        }
    }

    @Test
    fun givenOutdatedEventsFoundInDatabase_whenDeleteEventsByTime_thenEventsDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val whereClauseExpected =
            "${EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED} < '$outdatedTime'"

        mockCursorRecordsNumber(cursor, 2)
        every {
            database.query(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                columns = eq(EventsSchema.EventSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        } returns cursor
        every { cursor.getEvent() } returns event1 andThen event2

        // When
        val deletedEvents: List<EventDb> = SUT.deleteEventsByTime(outdatedTime)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                columns = eq(EventsSchema.EventSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        }
        verify(exactly = 1) {
            database.delete(
                EventsSchema.EventSchema.TABLE_NAME_EVENT,
                whereClauseExpected
            )
        }
        verify(exactly = 1) { database.cleanUnlinkedEvents() }

        Assert.assertEquals(listOf(event1, event2), deletedEvents)
    }

    @Test
    fun givenOutdatedEventsNotFoundInDatabase_whenDeleteEventsByTime_thenEventsNotDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val whereClauseExpected =
            "${EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED} < '$outdatedTime'"

        mockCursorRecordsNumber(cursor, 0)
        every {
            database.query(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                columns = eq(EventsSchema.EventSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        } returns cursor

        // When
        val deletedEvents: List<EventDb> = SUT.deleteEventsByTime(outdatedTime)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(EventsSchema.EventSchema.TABLE_NAME_EVENT),
                columns = eq(EventsSchema.EventSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        }
        verify(exactly = 1) {
            database.delete(
                EventsSchema.EventSchema.TABLE_NAME_EVENT,
                whereClauseExpected
            )
        }
        verify(exactly = 1) { database.cleanUnlinkedEvents() }

        Assert.assertEquals(emptyList<EventDb>(), deletedEvents)
    }


    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false
        every { cursorChild.isNull(any()) } returns false

        every { cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_ID) } returns COLUMN_INDEX_EVENTS_ID
        every { cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_DEVICE_ID) } returns COLUMN_INDEX_EVENTS_DEVICE_ID
        every { cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EVENTS_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID) } returns COLUMN_INDEX_EVENT_ROW_ID
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY) } returns COLUMN_INDEX_EVENT_TYPE_KEY
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED) } returns COLUMN_INDEX_EVENT_OCCURRED
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS) } returns COLUMN_INDEX_EVENT_PARAMS
        every { cursorChild.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID) } returns COLUMN_INDEX_EVENT_ROW_ID
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
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns(),
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
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                columns = EventsSchema.EventSchema.getAllColumns(),
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