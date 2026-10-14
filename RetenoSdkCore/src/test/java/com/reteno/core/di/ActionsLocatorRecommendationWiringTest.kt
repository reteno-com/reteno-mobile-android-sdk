package com.reteno.core.di

import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.RecordingRecommendationCallback
import com.reteno.core.fixtures.RecordingRecommendationJsonCallback
import com.reteno.core.fixtures.StubRecom
import com.reteno.core.fixtures.stubRecomEvents
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorRecommendationWiringTest {

    @Test
    fun `should load submitted typed recommendations`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)
        val callback = RecordingRecommendationCallback<StubRecom>()

        whenn()
        fixture.sut.submitRecommendationRequest("r1v2", REQUEST_BODY, StubRecom::class.java, callback)
        runCurrent()

        then()
        assertEquals(listOf(Recoms(listOf(StubRecom("sku-1")))), callback.results)
    }

    @Test
    fun `should load submitted raw recommendations`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)
        val callback = RecordingRecommendationJsonCallback()

        whenn()
        fixture.sut.submitRecommendationJsonRequest("r1v2", REQUEST_BODY, callback)
        runCurrent()

        then()
        assertEquals(listOf(RECOMS_JSON), callback.results)
    }

    @Test
    fun `should save submitted recommendation events`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitRecommendationEvents(stubRecomEvents())
        runCurrent()

        then()
        verify(exactly = 1) { fixture.recomEventsDb.insertRecomEvents(match { it.recomVariantId == "r1v2" }) }
    }

    @Test
    fun `should save submitted recommendation events one after another in call order`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitRecommendationEvents(stubRecomEvents(recomVariantId = "r1v2"))
        fixture.sut.submitRecommendationEvents(stubRecomEvents(recomVariantId = "r3v4"))
        runCurrent()

        then()
        verifyOrder {
            fixture.recomEventsDb.insertRecomEvents(match { it.recomVariantId == "r1v2" })
            fixture.recomEventsDb.insertRecomEvents(match { it.recomVariantId == "r3v4" })
        }
    }

    private companion object {
        const val REQUEST_BODY = """{"products":["sku-1"]}"""
        const val RECOMS_JSON = """{"recoms":[{"productId":"sku-1"}]}"""
    }
}
