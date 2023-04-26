package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.util.Util.formatToRemote
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.ZonedDateTime


class DbUtilRecomEventsTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val PRODUCT_ID_1 = "w12345s1345"
        private val RECOM_EVENT_TYPE_1 = RecomEventTypeDb.CLICKS
        private val RECOM_EVENT_OCCURRED_1 = ZonedDateTime.now().minusDays(1)

        private const val PRODUCT_ID_2 = "w999s999"
        private val RECOM_EVENT_TYPE_2 = RecomEventTypeDb.IMPRESSIONS
        private val RECOM_EVENT_OCCURRED_2 = ZonedDateTime.now().minusWeeks(1)


        private const val ROW_ID = "101"
        private const val RECOM_VARIANT_ID = "r111v333"
        private val RECOM_EVENTS_LIST = listOf(
            RecomEventDb(
                recomEventType = RECOM_EVENT_TYPE_1,
                occurred = RECOM_EVENT_OCCURRED_1.formatToRemote(),
                productId = PRODUCT_ID_1
            ),
            RecomEventDb(
                recomEventType = RECOM_EVENT_TYPE_2,
                occurred = RECOM_EVENT_OCCURRED_2.formatToRemote(),
                productId = PRODUCT_ID_2
            )
        )

        private const val COLUMN_INDEX_RECOM_VARIANT_ID = 1
        private const val COLUMN_INDEX_RECOM_EVENT_ROW_ID = 2
        private const val COLUMN_INDEX_RECOM_EVENT_PRODUCT_ID = 3
        private const val COLUMN_INDEX_RECOM_EVENT_OCCURRED = 4
        private const val COLUMN_INDEX_RECOM_EVENT_TYPE = 5
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
    fun givenRecomVariantIdProvided_whenPutRecomVariantId_thenContentValuesUpdated() {
        // Given
        val keySet = arrayOf(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID)

        // When
        contentValues.putRecomVariantId(RECOM_VARIANT_ID)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())
        assertEquals(
            RECOM_VARIANT_ID,
            contentValues.get(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID)
        )
    }

    @Test
    fun givenRecomEventsProvided_whenToContentValuesList_thenContentValuesListReturned() {
        // When
        val contentValuesList = RECOM_EVENTS_LIST.toContentValuesList(RECOM_VARIANT_ID)

        // Then
        val contentValues1 = contentValuesList[0]
        val contentValues2 = contentValuesList[1]

        assertEquals(
            RECOM_VARIANT_ID,
            contentValues1.get(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID)
        )
        assertEquals(
            RECOM_EVENT_TYPE_1.toString(),
            contentValues1.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE)
        )
        assertEquals(
            RECOM_EVENT_OCCURRED_1.formatToRemote(),
            contentValues1.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED)
        )
        assertEquals(
            PRODUCT_ID_1,
            contentValues1.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID)
        )

        assertEquals(
            RECOM_VARIANT_ID,
            contentValues2.get(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID)
        )
        assertEquals(
            RECOM_EVENT_TYPE_2.toString(),
            contentValues2.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE)
        )
        assertEquals(
            RECOM_EVENT_OCCURRED_2.formatToRemote(),
            contentValues2.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED)
        )
        assertEquals(
            PRODUCT_ID_2,
            contentValues2.get(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID)
        )
    }

    @Test
    fun givenEventFull_whenGetRecomEvent_thenRecomEventReturned() {
        // Given
        mockRecomEventFull()

        val expectedRecomEvent = RecomEventDb(
            rowId = ROW_ID,
            recomEventType = RECOM_EVENT_TYPE_1,
            occurred = RECOM_EVENT_OCCURRED_1.formatToRemote(),
            productId = PRODUCT_ID_1
        )

        // When
        val actualRecomEvent = cursor.getRecomEvent()

        // Then
        assertEquals(expectedRecomEvent, actualRecomEvent)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockRecomEventFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_EVENT_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_VARIANT_ID) } returns RECOM_VARIANT_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_EVENT_TYPE) } returns RECOM_EVENT_TYPE_1.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_EVENT_OCCURRED) } returns RECOM_EVENT_OCCURRED_1.formatToRemote()
        every { cursor.getStringOrNull(COLUMN_INDEX_RECOM_EVENT_PRODUCT_ID) } returns PRODUCT_ID_1
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID) } returns COLUMN_INDEX_RECOM_VARIANT_ID

        every { cursor.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID) } returns COLUMN_INDEX_RECOM_EVENT_ROW_ID
        every { cursor.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID) } returns COLUMN_INDEX_RECOM_EVENT_PRODUCT_ID
        every { cursor.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED) } returns COLUMN_INDEX_RECOM_EVENT_OCCURRED
        every { cursor.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE) } returns COLUMN_INDEX_RECOM_EVENT_TYPE
    }
    // endregion helper methods --------------------------------------------------------------------
}