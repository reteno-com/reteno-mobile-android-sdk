package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.fixtures.stubRecomEventsDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RecomMapperTest {

    @Test
    fun `should send recommendation request with its filters`() = runUnitTest {
        given()
        val request = RecomRequest(
            products = listOf("sku-1"),
            category = "shoes",
            fields = listOf("name"),
            filters = listOf(RecomFilter(name = "brand", values = listOf("acme")))
        )

        whenn()
        val remote = request.toRemote()

        then()
        val filter = requireNotNull(remote.filters).single()
        assertEquals(
            listOf(listOf("sku-1"), "shoes", listOf("name"), "brand", listOf("acme")),
            listOf(remote.products, remote.category, remote.fields, filter.name, filter.values)
        )
    }

    @Test
    fun `should send recommendation request without filters`() = runUnitTest {
        given()
        val request = RecomRequest(products = null, category = "shoes")

        whenn()
        val remote = request.toRemote()

        then()
        assertNull(remote.filters)
    }

    @Test
    fun `should send impressions and clicks of a recommendation variant separately`() = runUnitTest {
        given()
        val events = stubRecomEventsDb(
            recomEvents = listOf(
                RecomEventDb(recomEventType = RecomEventTypeDb.IMPRESSIONS, occurred = "t1", productId = "sku-1"),
                RecomEventDb(recomEventType = RecomEventTypeDb.CLICKS, occurred = "t2", productId = "sku-2")
            )
        )

        whenn()
        val remote = listOf(events).toRemote()

        then()
        val variant = remote.events.single()
        val impression = requireNotNull(variant.impressions).single()
        val click = requireNotNull(variant.clicks).single()
        assertEquals(
            listOf("variant-1", "t1", "sku-1", "t2", "sku-2"),
            listOf(variant.recomVariantId, impression.occurred, impression.productId, click.occurred, click.productId)
        )
    }

    @Test
    fun `should send variant without events as having no impressions and clicks`() = runUnitTest {
        given()
        val events = stubRecomEventsDb(recomEvents = null)

        whenn()
        val remote = events.toRemote()

        then()
        assertNull(remote.impressions)
        assertNull(remote.clicks)
    }
}
