package com.reteno.core.features.recommendation

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.RecordingRecommendationCallback
import com.reteno.core.fixtures.RecordingRecommendationJsonCallback
import com.reteno.core.fixtures.StubRecom
import com.reteno.core.fixtures.stubRecomEvents
import com.reteno.core.fixtures.stubRecomRequest
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class RecommendationImplTest {

    private class SutFixture {
        val actions = FakeSdkActions()
        val remoteLog = FakeRemoteLog()
        val sut = RecommendationImpl(actions, remoteLog)

        fun callEveryApi() {
            sut.fetchRecommendation(
                "r1v2",
                stubRecomRequest(),
                StubRecom::class.java,
                RecordingRecommendationCallback()
            )
            sut.fetchRecommendationJson("r3v4", stubRecomRequest(), RecordingRecommendationJsonCallback())
            sut.logRecommendations(stubRecomEvents(recomVariantId = "r5v6"))
        }
    }

    @Test
    fun `should pass every recommendation call to sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.callEveryApi()

        then()
        assertEquals(
            listOf(
                "submitRecommendationRequest r1v2 StubRecom $STUB_REQUEST_BODY",
                "submitRecommendationJsonRequest r3v4 $STUB_REQUEST_BODY",
                "submitRecommendationEvents r5v6"
            ),
            fixture.actions.calls
        )
    }

    @Test
    fun `should forward the same callback when fetching recommendations`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingRecommendationCallback<StubRecom>()

        whenn()
        fixture.sut.fetchRecommendation("r1v2", stubRecomRequest(), StubRecom::class.java, callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same callback when fetching recommendations as json`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingRecommendationJsonCallback()

        whenn()
        fixture.sut.fetchRecommendationJson("r1v2", stubRecomRequest(), callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same recommendation events`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val events = stubRecomEvents()

        whenn()
        fixture.sut.logRecommendations(events)

        then()
        assertSame(events, fixture.actions.recomEvents.single())
    }

    @Test
    fun `should send request as it was when called even if host changes its lists afterwards`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val products = mutableListOf("sku-1")
        val fields = mutableListOf("name")
        val request = RecomRequest(products, null, fields)

        whenn()
        fixture.sut.fetchRecommendationJson("r1v2", request, RecordingRecommendationJsonCallback())
        products.clear()
        fields.clear()

        then()
        val expected = """submitRecommendationJsonRequest r1v2 {"products":["sku-1"],"fields":["name"]}"""
        assertEquals(listOf(expected), fixture.actions.calls)
    }

    @Test
    fun `should keep host running and report every recommendation call that fails in sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.actions.failure = IllegalStateException("sdk broken")

        whenn()
        fixture.callEveryApi()

        then()
        assertEquals(
            listOf(
                "Recommendation.fetchRecommendation failed: IllegalStateException: sdk broken" to LogLevel.ERROR,
                "Recommendation.fetchRecommendationJson failed: IllegalStateException: sdk broken" to LogLevel.ERROR,
                "Recommendation.logRecommendations failed: IllegalStateException: sdk broken" to LogLevel.ERROR
            ),
            fixture.remoteLog.entries
        )
    }

    private companion object {
        const val STUB_REQUEST_BODY = """{"products":["sku-1"],"category":"shoes","fields":["name"],"filters":[""" +
            """{"name":"color","values":["red"]}]}"""
    }
}
