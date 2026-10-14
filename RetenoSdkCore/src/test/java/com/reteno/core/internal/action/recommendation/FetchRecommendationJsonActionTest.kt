package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.RecordingRecommendationJsonCallback
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FetchRecommendationJsonActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val callback = RecordingRecommendationJsonCallback(dispatchers.markedMain)
        val sut = FetchRecommendationJsonAction(rest.api(dispatchers), initGate, dispatchers, traced.runner)

        suspend fun fetch() = sut(VARIANT_ID, REQUEST_BODY, callback)
    }

    @Test
    fun `should send recommendation request for variant`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)

        whenn()
        fixture.fetch()

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.POST, request.method)
        assertEquals(ApiContract.Recommendation.GetRecoms(VARIANT_ID).url, request.contract.url)
        assertEquals(REQUEST_BODY, request.body)
    }

    @Test
    fun `should pass raw recommendations to host callback on main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)

        whenn()
        val result = fixture.fetch()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(RECOMS_JSON), fixture.callback.results)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
    }

    @Test
    fun `should pass server failure to host callback without reporting it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(500, "server error")

        whenn()
        val result = fixture.fetch()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(500, "server error", null)), fixture.callback.failures)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should wait for sdk initialization before requesting recommendations`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.rest.succeedWith(RECOMS_JSON)
        launch { fixture.fetch() }
        runCurrent()
        val requestsBeforeInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(0, requestsBeforeInit)
        assertEquals(listOf(RECOMS_JSON), fixture.callback.results)
    }

    @Test
    fun `should trace variant without server details`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)

        whenn()
        fixture.fetch()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("FetchRecommendationJson  recomVariantId=$VARIANT_ID"))
        assertFalse(lines.any { it.contains("http") || it.contains("Sneakers") })
    }

    private companion object {
        const val VARIANT_ID = "r1v2"
        const val REQUEST_BODY = """{"products":["sku-1"]}"""
        const val RECOMS_JSON = """{"recoms":[{"productId":"sku-1","name":"Sneakers"}]}"""
    }
}
