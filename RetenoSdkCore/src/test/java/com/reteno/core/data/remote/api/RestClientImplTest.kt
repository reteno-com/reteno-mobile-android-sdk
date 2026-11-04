package com.reteno.core.data.remote.api

import com.reteno.core.BuildConfig
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.fixtures.FakeDeviceIdSources
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.LocalHttpServer
import com.reteno.core.fixtures.STUB_ACCESS_KEY
import com.reteno.core.fixtures.ScriptedResponse
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.util.Util
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal class RestClientImplTest {

    private class SutFixture(val server: LocalHttpServer) {
        val logs = FakeLogSink()
        val callback = RecordingResponseCallback()
        val prefs = mockk<SharedPrefsManager>(relaxed = true)
        val restConfig = RestConfig(prefs, DeviceIdHelper(FakeDeviceIdSources(), prefs) { stubConfig() })
        val sut = RestClientImpl(restConfig, { stubConfig() }, logs, server.connections)

        fun request(
            contract: ApiContract = ApiContract.MobileApi.Device,
            method: HttpMethod = HttpMethod.GET,
            body: String? = null,
            headers: Map<String, String>? = null,
            queryParams: Map<String, String?>? = null,
            retryCount: Int = 0
        ) {
            sut.makeRequest(method, contract, body, headers, queryParams, retryCount, callback)
        }

        fun <T> withDebugView(block: () -> T): T {
            val before = Util.isDebugView()
            Util.setIsDebug(true)
            try {
                return block()
            } finally {
                Util.setIsDebug(before)
            }
        }
    }

    private class RecordingResponseCallback : ResponseCallback {
        val successes = mutableListOf<Pair<Map<String, List<String>>, String>>()
        val failures = mutableListOf<Triple<Int?, String?, Throwable?>>()

        override fun onSuccess(headers: Map<String, List<String>>, response: String) {
            successes += headers to response
        }

        override fun onSuccess(response: String) {
            error("headers are always passed")
        }

        override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            failures += Triple(statusCode, response, throwable)
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) {
        LocalHttpServer.serve { server -> SutFixture(server).block() }
    }

    @Test
    fun `should identify sdk to mobile api without device id`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(contract = ApiContract.MobileApi.Device)

            then()
            val sent = server.requests.single()
            assertEquals(listOf("GET", "/api/v1/device"), listOf(sent.method, sent.target))
            assertEquals(STUB_ACCESS_KEY, sent.header("X-Reteno-Access-Key"))
            assertEquals("${stubConfig().platform} ${BuildConfig.SDK_VERSION}", sent.header("X-Reteno-SDK-Version"))
            assertNull(sent.header("X-Reteno-Device-ID"))
        }
    }

    @Test
    fun `should identify device to app inbox`() = runUnitTest {
        withSut {
            given()
            restConfig.initDeviceId(DeviceIdMode.ANDROID_ID)
            server.answer(ScriptedResponse(200, "{}"))

            whenn()
            request(contract = ApiContract.AppInbox.Messages)

            then()
            val sent = server.requests.single()
            assertEquals(restConfig.deviceId.id, sent.header("X-Reteno-Device-ID"))
            assertEquals(STUB_ACCESS_KEY, sent.header("X-Reteno-Access-Key"))
        }
    }

    @Test
    fun `should identify device to in-app messages and recommendations`() = runUnitTest {
        withSut {
            given()
            restConfig.initDeviceId(DeviceIdMode.ANDROID_ID)
            server.answer(ScriptedResponse(200, "{}"), ScriptedResponse(200, "{}"))

            whenn()
            request(contract = ApiContract.InAppMessages.GetInAppMessages)
            request(contract = ApiContract.Recommendation.PostRecoms, method = HttpMethod.POST, body = "{}")

            then()
            val deviceIds = server.requests.map { it.header("X-Reteno-Device-ID") }
            assertEquals(listOf(restConfig.deviceId.id, restConfig.deviceId.id), deviceIds)
        }
    }

    @Test
    fun `should not send access key or device id to custom url`() = runUnitTest {
        withSut {
            given()
            restConfig.initDeviceId(DeviceIdMode.ANDROID_ID)
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(contract = ApiContract.Custom("https://links.example/click"))

            then()
            val sent = server.requests.single()
            val identifying = listOf("X-Reteno-Access-Key", "X-Reteno-SDK-Version", "X-Reteno-Device-ID")
            assertEquals(listOf(null, null, null), identifying.map { sent.header(it) })
            assertEquals("/click", sent.target)
        }
    }

    @Test
    fun `should accept any json response`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request()

            then()
            val sent = server.requests.single()
            val expected = listOf("*/*", "application/json; charset=UTF-8", null, null)
            val names = listOf("Accept", "Content-Type", "Accept-Encoding", "X-Reteno-Debug")
            assertEquals(expected, names.map { sent.header(it) })
        }
    }

    @Test
    fun `should mark requests made while debug view is on`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            withDebugView { request() }

            then()
            assertEquals("true", server.requests.single().header("X-Reteno-Debug"))
        }
    }

    @Test
    fun `should stream body of a post request`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(method = HttpMethod.POST, body = BODY_WITH_CYRILLIC)

            then()
            val sent = server.requests.single()
            val expected = listOf("POST", BODY_WITH_CYRILLIC, "chunked", "gzip")
            val actual = listOf(sent.method, sent.body, sent.header("Transfer-Encoding"), sent.header("Accept-Encoding"))
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `should send caller headers`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(headers = mapOf("If-None-Match" to "etag-1", "Accept" to "text/html"))

            then()
            val sent = server.requests.single()
            assertEquals(listOf("etag-1", "text/html"), listOf(sent.header("If-None-Match"), sent.header("Accept")))
        }
    }

    @Test
    fun `should encode query params and skip absent ones`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(queryParams = mapOf("page" to "1", "status" to null, "q" to "a b&c=д"))

            then()
            assertEquals("/api/v1/device?page=1&q=a%20b%26c%3D%D0%B4", server.requests.single().target)
        }
    }

    @Test
    fun `should append query params to url that already has a query`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(
                contract = ApiContract.Custom("https://links.example/go?id=7"),
                queryParams = mapOf("page" to "2")
            )

            then()
            assertEquals("/go?id=7&page=2", server.requests.single().target)
        }
    }

    @Test
    fun `should leave url as it is when every query param is absent`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(queryParams = mapOf("status" to null))

            then()
            assertEquals("/api/v1/device", server.requests.single().target)
        }
    }

    @Test
    fun `should pass ok response with its headers`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "<html/>", mapOf("x-amz-meta-version" to "v7")))

            whenn()
            request()

            then()
            val (headers, body) = callback.successes.single()
            assertEquals("<html/>", body)
            assertEquals(listOf("v7"), headers["x-amz-meta-version"])
            assertTrue(callback.failures.isEmpty())
        }
    }

    @Test
    fun `should pass ok response without body as empty text`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200))

            whenn()
            request()

            then()
            assertEquals("", callback.successes.single().second)
        }
    }

    @Test
    fun `should follow redirect to its target`() = runUnitTest {
        withSut {
            given()
            server.answer(
                ScriptedResponse(302, headers = mapOf("Location" to "/moved")),
                ScriptedResponse(200, "moved body")
            )

            whenn()
            request()

            then()
            assertEquals("moved body", callback.successes.single().second)
            assertEquals("/moved", server.requests.last().target)
        }
    }

    @Test
    fun `should pass redirect that cannot be followed as success`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(301, "permanent"), ScriptedResponse(302, "found"))

            whenn()
            request()
            request()

            then()
            assertEquals(listOf("permanent", "found"), callback.successes.map { it.second })
        }
    }

    @Test
    fun `should report client error with its body`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(400, ERROR_BODY))

            whenn()
            request()

            then()
            assertEquals(Triple(400, ERROR_BODY, null), callback.failures.single())
            assertTrue(callback.successes.isEmpty())
        }
    }

    @Test
    fun `should report other success status as failure without body`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(201, "created"))

            whenn()
            request()

            then()
            assertEquals(Triple(201, null, null), callback.failures.single())
        }
    }

    @Test
    fun `should report server error when no retry is left`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(503, "busy"))

            whenn()
            request(retryCount = 0)

            then()
            assertEquals(Triple(503, "busy", null), callback.failures.single())
        }
    }

    @Test
    fun `should retry server error and pass the next answer`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(500, "down"), ScriptedResponse(200, "ok"))

            whenn()
            request(retryCount = 1)

            then()
            assertEquals("ok", callback.successes.single().second)
            assertEquals(2, server.requests.size)
            assertTrue(callback.failures.isEmpty())
        }
    }

    @Test
    fun `should retry server error as many times as allowed`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(500, "down"), ScriptedResponse(503, "busy"), ScriptedResponse(200, "ok"))

            whenn()
            request(retryCount = 2)

            then()
            assertEquals("ok", callback.successes.single().second)
            assertEquals(3, server.requests.size)
        }
    }

    @Test
    fun `should retry timed out request`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(408), ScriptedResponse(200, "ok"))

            whenn()
            request(retryCount = 3)

            then()
            assertEquals("ok", callback.successes.single().second)
        }
    }

    @Test
    fun `should report last server error when retries run out`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(500, "first"), ScriptedResponse(502, "second"))

            whenn()
            request(retryCount = 1)

            then()
            assertEquals(Triple(502, "second", null), callback.failures.single())
            assertEquals(2, server.requests.size)
        }
    }

    @Test
    fun `should not retry client error`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(404, "missing"), ScriptedResponse(200, "ok"))

            whenn()
            request(retryCount = 2)

            then()
            assertEquals(Triple(404, "missing", null), callback.failures.single())
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun `should report connection that drops without answer`() = runUnitTest {
        withSut {
            given()
            server.answer()

            whenn()
            request(method = HttpMethod.POST, body = "{}")

            then()
            val (code, body, error) = callback.failures.single()
            assertEquals(listOf(null, null), listOf(code, body))
            assertTrue(error is IOException)
        }
    }

    @Test
    fun `should configure timeouts and disable caches`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, "ok"))

            whenn()
            request(method = HttpMethod.PUT, body = "{}")

            then()
            val connection = server.opened.single()
            val expected = listOf<Any>(15_000, 10_000, "PUT", false, true)
            val actual = with(connection) {
                listOf<Any>(readTimeout, connectTimeout, requestMethod, useCaches, instanceFollowRedirects)
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `should log only method and status of a request`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, RESPONSE_WITH_PII))

            whenn()
            request(contract = ApiContract.AppInbox.Messages, method = HttpMethod.POST, body = REQUEST_WITH_PII)

            then()
            assertEquals(listOf("POST → 200"), logs.lines)
        }
    }

    @Test
    fun `should log failed request without url or body`() = runUnitTest {
        given()
        val logs = FakeLogSink()
        val callback = RecordingResponseCallback()
        val failingConnections = HttpConnections { url -> throw IOException("Unable to reach $url") }
        val prefs = mockk<SharedPrefsManager>(relaxed = true)
        val restConfig = RestConfig(prefs, DeviceIdHelper(FakeDeviceIdSources(), prefs) { stubConfig() })
        val sut = RestClientImpl(restConfig, { stubConfig() }, logs, failingConnections)

        whenn()
        sut.makeRequest(HttpMethod.POST, ApiContract.MobileApi.Device, REQUEST_WITH_PII, null, null, 0, callback)

        then()
        assertEquals(listOf("POST failed: IOException: Unable to reach <url>"), logs.lines)
        assertTrue(callback.failures.single().third is IOException)
    }

    @Test
    fun `should ignore socket factory the app installed for all https connections`() = runUnitTest {
        given()
        val appFactory = mockk<SSLSocketFactory>()
        val before = HttpsURLConnection.getDefaultSSLSocketFactory()
        HttpsURLConnection.setDefaultSSLSocketFactory(appFactory)

        whenn()
        val connection = try {
            HttpsConnections.open("https://mobile-api.reteno.com/api/v1/device") as HttpsURLConnection
        } finally {
            HttpsURLConnection.setDefaultSSLSocketFactory(before)
        }

        then()
        assertNotSame(appFactory, connection.sslSocketFactory)
        assertEquals(SSLSocketFactory.getDefault().javaClass, connection.sslSocketFactory.javaClass)
    }

    @Test
    fun `should refuse plain http connection`() = runUnitTest {
        given()
        val url = "http://mobile-api.reteno.com/api/v1/device"

        whenn()
        val opened = runCatching { HttpsConnections.open(url) }

        then()
        assertTrue(opened.exceptionOrNull() is ClassCastException)
    }

    companion object {
        private const val BODY_WITH_CYRILLIC = """{"name":"Ліна"}"""
        private const val ERROR_BODY = """{"error":"bad"}"""
        private const val REQUEST_WITH_PII = """{"email":"john.doe@example.com","phone":"+380501234567"}"""
        private const val RESPONSE_WITH_PII = """{"firstName":"John","pushToken":"fcm-token-abcdef"}"""
    }
}
