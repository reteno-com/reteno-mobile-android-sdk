package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.remote.model.recommendation.get.RecomFilterRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomRequestRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRequestRemote
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import org.junit.Assert.assertEquals

import org.junit.Test


class RecomMapperKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val RECOM_ROW_ID = "ROW_ID"

        private const val RECOM_FILTER_NAME = "RECOM_FILTER_NAME"
        private const val RECOM_FILTER_VALUE_1 = "RECOM_FILTER_VALUE_1"
        private const val RECOM_FILTER_VALUE_2 = "RECOM_FILTER_VALUE_2"
        private val RECOM_FILTER_VALUES = listOf(RECOM_FILTER_VALUE_1, RECOM_FILTER_VALUE_2)

        private const val RECOM_PRODUCT_1 = "RECOM_PRODUCT_1"
        private const val RECOM_PRODUCT_2 = "RECOM_PRODUCT_2"
        private val RECOM_PRODUCTS = listOf(RECOM_PRODUCT_1, RECOM_PRODUCT_2)
        private const val RECOM_CATEGORY = "RECOM_CATEGORY"
        private const val RECOM_FIELD_1 = "RECOM_FIELD_1"
        private const val RECOM_FIELD_2 = "RECOM_FIELD_2"
        private val RECOM_FIELDS = listOf(RECOM_FIELD_1, RECOM_FIELD_2)

        private const val RECOM_VARIANT_ID = "r111v333"
        private const val PRODUCT_ID_1 = "w12345s1345"
        private val RECOM_EVENT_TYPE_1 = RecomEventTypeDb.IMPRESSIONS
        private const val RECOM_EVENT_OCCURRED_1 = "OCCURRED_1"
        private const val PRODUCT_ID_2 = "w999s999"
        private val RECOM_EVENT_TYPE_2 = RecomEventTypeDb.CLICKS
        private const val RECOM_EVENT_OCCURRED_2 = "OCCURRED_2"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenRecomFilter_whenToRemote_thenRecomFilterRemoteReturned() {
        // Given
        val recomFilter = getRecomFilter()
        val expectedRecomFilterRemote = getRecomFilterRemote()

        // When
        val actualRecomFilterRemote = recomFilter.map { it.toRemote() }

        // Then
        assertEquals(expectedRecomFilterRemote, actualRecomFilterRemote)
    }

    @Test
    fun givenRecomRequest_whenToRemote_thenRecomRequestRemoteReturned() {
        // Given
        val recomRequest = getRecomRequest()
        val expectedRecomRequestRemote = getRecomRequestRemote()

        // When
        val actualRecomRequestRemote = recomRequest.toRemote()

        // Then
        assertEquals(expectedRecomRequestRemote, actualRecomRequestRemote)
    }

    @Test
    fun givenRecomEventDb_whenToRemote_thenRecomEventRemoteReturned() {
        // Given
        val recomEventDb = getRecomEventDb()
        val expectedRecomEventRemote = getRecomEventRemote1()

        // When
        val actualRecomEventRemote = recomEventDb.toRemote()

        // Then
        assertEquals(expectedRecomEventRemote, actualRecomEventRemote)
    }

    @Test
    fun givenRecomEventsDb_whenToRemote_thenRecomEventsRemoteReturned() {
        // Given
        val recomEventsDb = getRecomEventsDb()
        val expectedRecomEventsRemote = getRecomEventsRemote()

        // When
        val actualRecomEventsRemote = recomEventsDb.toRemote()

        // Then
        assertEquals(expectedRecomEventsRemote, actualRecomEventsRemote)
    }

    @Test
    fun givenRecomEventsListDb_whenToRemote_thenRecomEventsListRemoteReturned() {
        // Given
        val recomEventsListDb = listOf(getRecomEventsDb())
        val expectedRecomEventsListRemote = RecomEventsRequestRemote(
            events = listOf(getRecomEventsRemote())
        )

        // When
        val actualRecomEventsListRemote = recomEventsListDb.toRemote()

        // Then
        assertEquals(expectedRecomEventsListRemote, actualRecomEventsListRemote)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getRecomFilter() = listOf(
        RecomFilter(
            name = RECOM_FILTER_NAME,
            values = RECOM_FILTER_VALUES
        )
    )

    private fun getRecomFilterRemote() = listOf(
        RecomFilterRemote(
            name = RECOM_FILTER_NAME,
            values = RECOM_FILTER_VALUES
        )
    )

    private fun getRecomRequest() = RecomRequest(
        RECOM_PRODUCTS,
        RECOM_CATEGORY,
        RECOM_FIELDS,
        getRecomFilter()
    )

    private fun getRecomRequestRemote() = RecomRequestRemote(
        RECOM_PRODUCTS,
        RECOM_CATEGORY,
        RECOM_FIELDS,
        getRecomFilterRemote()
    )

    private fun getRecomEventDb() = RecomEventDb(
        rowId = RECOM_ROW_ID,
        recomEventType = RECOM_EVENT_TYPE_1,
        occurred = RECOM_EVENT_OCCURRED_1,
        productId = PRODUCT_ID_1
    )

    private fun getRecomEventRemote1() = RecomEventRemote(
        occurred = RECOM_EVENT_OCCURRED_1,
        productId = PRODUCT_ID_1
    )

    private fun getRecomEventRemote2() = RecomEventRemote(
        occurred = RECOM_EVENT_OCCURRED_2,
        productId = PRODUCT_ID_2
    )

    private fun getRecomEventsDb() = RecomEventsDb(
        recomVariantId = RECOM_VARIANT_ID,
        recomEvents = listOf(
            RecomEventDb(
                recomEventType = RECOM_EVENT_TYPE_1,
                occurred = RECOM_EVENT_OCCURRED_1,
                productId = PRODUCT_ID_1
            ),
            RecomEventDb(
                recomEventType = RECOM_EVENT_TYPE_2,
                occurred = RECOM_EVENT_OCCURRED_2,
                productId = PRODUCT_ID_2
            )
        )
    )

    private fun getRecomEventsRemote() = RecomEventsRemote(
        recomVariantId = RECOM_VARIANT_ID,
        impressions = listOf(getRecomEventRemote1()),
        clicks = listOf(getRecomEventRemote2())
    )
    // endregion helper methods --------------------------------------------------------------------
}