package com.reteno.core.internal.network

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.fixtures.CountingDispatcher
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class SuspendApiClientTest {

    private class SutFixture(testScope: TestScope) {
        val restClient = mockk<RestClient>()
        private val base = testScope.testDispatchers()
        val io = CountingDispatcher(base.io)
        val dispatchers = object : RetenoDispatchers {
            override val main: CoroutineDispatcher = base.main
            override val io: CoroutineDispatcher = this@SutFixture.io
            override val default: CoroutineDispatcher = base.default
        }
        val sut = SuspendApiClient(restClient, dispatchers)

        fun respond(answer: (ResponseCallback) -> Unit) {
            every { restClient.makeRequest(any(), any(), any(), any(), any(), any(), any()) } answers {
                answer(arg(6))
            }
        }
    }

    @Test
    fun `should return success with body and headers`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val headers = mapOf("etag" to listOf("v1"))
        fixture.respond { it.onSuccess(headers, "{}") }

        whenn()
        val result = fixture.sut.request(HttpMethod.GET, ApiContract.MobileApi.Device)

        then()
        assertEquals(ApiResult.Success(body = "{}", headers = headers), result)
    }

    @Test
    fun `should return success with empty headers when callback has no headers`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.respond { it.onSuccess("ok") }

        whenn()
        val result = fixture.sut.request(HttpMethod.GET, ApiContract.MobileApi.Device)

        then()
        assertEquals(ApiResult.Success(body = "ok", headers = emptyMap()), result)
    }

    @Test
    fun `should return failure with status body and error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("offline")
        fixture.respond { it.onFailure(null, "body", error) }

        whenn()
        val result = fixture.sut.request(HttpMethod.POST, ApiContract.MobileApi.Device, body = "{}")

        then()
        val failure = result as ApiResult.Failure
        assertNull(failure.statusCode)
        assertEquals("body", failure.body)
        assertSame(error, failure.error)
    }

    @Test
    fun `should return failure when rest client never calls back`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.respond { }

        whenn()
        val result = fixture.sut.request(HttpMethod.GET, ApiContract.MobileApi.Device)

        then()
        val failure = result as ApiResult.Failure
        assertTrue(failure.error is IllegalStateException)
        assertTrue(failure.isRepeatable)
    }

    @Test
    fun `should pass every request parameter to rest client`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.respond { it.onSuccess("ok") }
        val headers = mapOf("h" to "v")
        val query = mapOf("q" to "1")

        whenn()
        fixture.sut.request(HttpMethod.PUT, ApiContract.MobileApi.User, "{}", headers, query, retryCount = 3)

        then()
        verify(exactly = 1) {
            fixture.restClient.makeRequest(HttpMethod.PUT, ApiContract.MobileApi.User, "{}", headers, query, 3, any())
        }
    }

    @Test
    fun `should perform blocking request on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.respond { it.onSuccess("ok") }

        whenn()
        fixture.sut.request(HttpMethod.GET, ApiContract.MobileApi.Device)

        then()
        assertTrue(fixture.io.dispatchCount > 0)
    }

    @Test
    fun `should treat server errors and too many requests as repeatable`() = runUnitTest {
        given()
        val codes = listOf(500, 503, 429)

        whenn()
        val repeatable = codes.map { ApiResult.Failure(it, null, null).isRepeatable }

        then()
        assertEquals(listOf(true, true, true), repeatable)
    }

    @Test
    fun `should treat client errors as non repeatable`() = runUnitTest {
        given()
        val failure = ApiResult.Failure(404, null, null)

        whenn()
        val repeatable = failure.isRepeatable

        then()
        assertFalse(repeatable)
    }
}
