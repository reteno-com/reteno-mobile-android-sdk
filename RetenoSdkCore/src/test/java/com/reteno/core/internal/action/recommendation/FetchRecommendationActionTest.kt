package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.RecordingRecommendationCallback
import com.reteno.core.fixtures.StubRecom
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
internal class FetchRecommendationActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val callback = RecordingRecommendationCallback<StubRecom>(dispatchers.markedMain)
        val sut = FetchRecommendationAction(rest.api(dispatchers), initGate, dispatchers, traced.runner)

        suspend fun fetch() = sut(VARIANT_ID, REQUEST_BODY, StubRecom::class.java, callback)
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
    fun `should encode variant id in request path`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)

        whenn()
        fixture.sut("r1v2/../../user?x=#", REQUEST_BODY, StubRecom::class.java, fixture.callback)

        then()
        val url = fixture.rest.requests.single().contract.url
        assertTrue(url.endsWith("/recoms/r1v2%2F..%2F..%2Fuser%3Fx%3D%23/request"))
    }

    @Test
    fun `should pass typed recommendations to host callback on main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(RECOMS_JSON)

        whenn()
        val result = fixture.fetch()

        then()
        assertTrue(result.isSuccess)
        val expected = Recoms(listOf(StubRecom("sku-1", "Sneakers"), StubRecom("sku-2", "Boots")))
        assertEquals(listOf(expected), fixture.callback.results)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
    }

    @Test
    fun `should fall back to raw response when recommendations cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("{}")

        whenn()
        val result = fixture.fetch()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("{}"), fixture.callback.rawResponses)
        assertTrue(fixture.callback.results.isEmpty())
        assertEquals(listOf(true), fixture.callback.calledOnMain)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should pass server failure to host callback without reporting it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(404, "unknown variant")

        whenn()
        val result = fixture.fetch()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(404, "unknown variant", null)), fixture.callback.failures)
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
        assertEquals(1, fixture.callback.results.size)
    }

    @Test
    fun `should neither request nor answer host when cancelled before initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        val fetch = launch { fixture.fetch() }
        runCurrent()

        whenn()
        fetch.cancel()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.callback.calledOnMain.isEmpty())
        assertTrue(fixture.traced.sink.lines.last().contains("⊘ CANCELLED"))
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
        assertTrue(lines.first().contains("FetchRecommendation  recomVariantId=$VARIANT_ID  responseClass=StubRecom"))
        assertFalse(lines.any { it.contains("http") || it.contains("Sneakers") })
    }

    private companion object {
        const val VARIANT_ID = "r1v2"
        const val REQUEST_BODY = """{"products":["sku-1"]}"""
        const val RECOMS_JSON =
            """{"recoms":[{"productId":"sku-1","name":"Sneakers"},{"productId":"sku-2","name":"Boots"}]}"""
    }
}
