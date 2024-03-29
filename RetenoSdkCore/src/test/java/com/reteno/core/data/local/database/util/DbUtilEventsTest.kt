package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import java.time.ZonedDateTime


class DbUtilEventsTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val PARENT_ROW_ID = 1L
        private const val EVENT_ROW_ID = "131"

        private const val DEVICE_ID = "deviceId1"
        private const val EXTERNAL_USER_ID = "externalUserId1"

        private const val EVENT_1_TYPE_KEY = "key1"
        private val EVENT_1_OCCURRED = ZonedDateTime.now().minusDays(1).toString()

        private const val EVENT_2_TYPE_KEY = "key2"
        private val EVENT_2_OCCURRED = ZonedDateTime.now().toString()

        private const val EVENT_3_TYPE_KEY = "key3"
        private val EVENT_3_OCCURRED = ZonedDateTime.now().minusDays(2).toString()

        private const val FIELD_PARAM_NAME_1 = "name1"
        private const val FIELD_PARAM_VALUE_1 = "value1"
        private const val FIELD_PARAM_NAME_2 = "name2"
        private const val FIELD_PARAM_VALUE_2 = "value"

        private const val COLUMN_INDEX_DEVICE_ID = 1
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 2

        private const val COLUMN_INDEX_EVENT_ROW_ID = 3
        private const val COLUMN_INDEX_EVENT_TYPE_KEY = 4
        private const val COLUMN_INDEX_EVENT_OCCURRED = 5
        private const val COLUMN_INDEX_EVENT_PARAMS = 6
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
    fun givenEventsProvided_whenPutEvents_thenContentValuesUpdated() {
        // Given
        val events = EventsDb(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            eventList = listOf()
        )

        val keySet = arrayOf(
            EventsSchema.COLUMN_EVENTS_DEVICE_ID,
            EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID,
        )

        // When
        contentValues.putEvents(events)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(EventsSchema.COLUMN_EVENTS_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID))
    }

    @Test
    fun givenEventsProvided_whenToContentValuesList_thenContentValuesListReturned() {
        // Given
        val parameter1 = ParameterDb(name = FIELD_PARAM_NAME_1, value = FIELD_PARAM_VALUE_1)
        val parameter2 = ParameterDb(name = FIELD_PARAM_NAME_2, value = FIELD_PARAM_VALUE_2)
        val params1 = listOf(parameter1, parameter2)
        val params2 = listOf(parameter2, parameter1)

        val event1 = EventDb(eventTypeKey = EVENT_1_TYPE_KEY, occurred = EVENT_1_OCCURRED, params = params1)
        val event2 = EventDb(eventTypeKey = EVENT_2_TYPE_KEY, occurred = EVENT_2_OCCURRED, params = params2)
        val event3 = EventDb(eventTypeKey = EVENT_3_TYPE_KEY, occurred = EVENT_3_OCCURRED, params = null)
        val eventList = listOf(event1, event2, event3)

        val expectedParams1 = "[${getExpectedParam1()},${getExpectedParam2()}]"
        val expectedParams2 = "[${getExpectedParam2()},${getExpectedParam1()}]"

        // When
        val contentValuesList = eventList.toContentValuesList(PARENT_ROW_ID)

        // Then
        val contentValues1 = contentValuesList[0]
        val contentValues2 = contentValuesList[1]
        val contentValues3 = contentValuesList[2]

        assertEquals(EVENT_1_TYPE_KEY, contentValues1.get(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY))
        assertEquals(EVENT_1_OCCURRED.toString(), contentValues1.get(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED))
        assertEquals(expectedParams1, contentValues1.get(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS))

        assertEquals(EVENT_2_TYPE_KEY, contentValues2.get(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY))
        assertEquals(EVENT_2_OCCURRED.toString(), contentValues2.get(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED))
        assertEquals(expectedParams2, contentValues2.get(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS))

        assertEquals(EVENT_3_TYPE_KEY, contentValues3.get(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY))
        assertEquals(EVENT_3_OCCURRED.toString(), contentValues3.get(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED))
        assertNull(contentValues3.get(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS))
    }

    @Test
    fun givenEventFull_whenGetEvent_thenEventReturned() {
        // Given
        mockEventFull()

        val expectedParams = listOf(ParameterDb(FIELD_PARAM_NAME_1, FIELD_PARAM_VALUE_1), ParameterDb(
            FIELD_PARAM_NAME_2, FIELD_PARAM_VALUE_2
        ))
        val expectedEvent = EventDb(
            rowId = EVENT_ROW_ID,
            eventTypeKey = EVENT_1_TYPE_KEY,
            occurred = EVENT_1_OCCURRED,
            params = expectedParams
        )

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertEquals(expectedEvent, actualEvent)
    }

    @Test
    fun givenEventParamsEmpty_whenGetEvent_thenEventReturned() {
        // Given
        mockEventParamsEmpty()

        val expectedEvent = EventDb(
            rowId = EVENT_ROW_ID,
            eventTypeKey = EVENT_1_TYPE_KEY,
            occurred = EVENT_1_OCCURRED,
            params = listOf()
        )

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertEquals(expectedEvent, actualEvent)
    }

    @Test
    fun givenEventParamsNull_whenGetEvent_thenEventReturned() {
        // Given
        mockEventParamsNull()

        val expectedEvent = EventDb(
            rowId = EVENT_ROW_ID,
            eventTypeKey = EVENT_1_TYPE_KEY,
            occurred = EVENT_1_OCCURRED,
            params = null
        )

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertEquals(expectedEvent, actualEvent)
    }

    @Test
    fun givenEventTypeNull_whenGetEvent_thenNullReturned() {
        // Given
        mockEventTypeNull()

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertNull(actualEvent)
    }

    @Test
    fun givenEventOccurredNull_whenGetEvent_thenNullReturned() {
        // Given
        mockEventOccurredNull()

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertNull(actualEvent)
    }

    @Test
    fun givenEventEmpty_whenGetEvent_thenNullReturned() {
        // Given
        mockEventEmpty()

        // When
        val actualEvent = cursor.getEvent()

        // Then
        assertNull(actualEvent)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getExpectedFullParams(): String = "[${getExpectedParam1()},${getExpectedParam2()}]"

    private fun getExpectedParam1() =
        "{\"name\":\"$FIELD_PARAM_NAME_1\",\"value\":\"$FIELD_PARAM_VALUE_1\"}"

    private fun getExpectedParam2() =
        "{\"name\":\"$FIELD_PARAM_NAME_2\",\"value\":\"$FIELD_PARAM_VALUE_2\"}"

    private fun mockEventFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns EVENT_ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns EVENT_1_TYPE_KEY
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns EVENT_1_OCCURRED.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns getExpectedFullParams()
    }

    private fun mockEventParamsEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns EVENT_ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns EVENT_1_TYPE_KEY
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns EVENT_1_OCCURRED.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns "[]"
    }

    private fun mockEventParamsNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns EVENT_ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns EVENT_1_TYPE_KEY
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns EVENT_1_OCCURRED.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns null
    }

    private fun mockEventTypeNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns EVENT_1_OCCURRED.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns null
    }

    private fun mockEventOccurredNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns EVENT_1_TYPE_KEY
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns null
    }

    private fun mockEventEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_ROW_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_TYPE_KEY) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_OCCURRED) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EVENT_PARAMS) } returns null
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID

        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID) } returns COLUMN_INDEX_EVENT_ROW_ID
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY) } returns COLUMN_INDEX_EVENT_TYPE_KEY
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED) } returns COLUMN_INDEX_EVENT_OCCURRED
        every { cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS) } returns COLUMN_INDEX_EVENT_PARAMS
    }
    // endregion helper methods --------------------------------------------------------------------
}