package com.reteno.tests.data

import android.net.Uri
import android.util.Log
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.ConnectionManager
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.domain.ResponseCallback
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

@RunWith(JUnit4::class)
class RetenoRestClientTest {

    companion object {
        private const val TEST_URL = "http://www.test.com"
        private const val TEST_RESPONSE = "test response"
        private const val TEST_ERROR_RESPONSE = "error response"
    }

    @RelaxedMockK
    private lateinit var httpURLConnection: HttpURLConnection

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(ConnectionManager)
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkObject(ConnectionManager)
        unmockkStatic(Log::class)
    }

    @Test
    fun appendedQueryParams_paramsNotNull() {
        val expectedUrl = "http://www.test.com?params1=false&params2=9"
        val params = mapOf("params1" to false, "params2" to 9)

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        val mockBuilder = mockk<Uri.Builder>()
        every { Uri.parse(any()) } returns mockUri
        every { mockUri.buildUpon() } returns mockBuilder
        every { mockUri.toString() } returns generateUriWithParams(params)
        every { mockBuilder.appendQueryParameter(any(), any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockUri

        makeRequest(HttpMethod.GET, queryParams = params)

        verify { mockBuilder.appendQueryParameter(any(), any()) }
        verify { ConnectionManager.openConnection(eq(expectedUrl)) }
    }

    @Test
    fun appendedQueryParams_paramsAreEmpty() {
        val params = emptyMap<String, Any>()
        val expectedUrl = "http://www.test.com"

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        mockkStatic(Uri::class)

        makeRequest(HttpMethod.GET, queryParams = params)

        verify(inverse = true) { Uri.parse(any()) }
        verify { ConnectionManager.openConnection(eq(expectedUrl)) }
    }

    @Test
    fun addedAuthorizationHeaders_callMobileApi() {
        val url = ApiContract.MobileApi.Events
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(url = url)

        verify { httpURLConnection.setRequestProperty(eq(RetenoRestClientProxy.HEADER_KEY), any()) }
        verify {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_VERSION),
                any()
            )
        }
    }

    @Test
    fun didntAddedAuthorizationHeaders_callIsNotMobileApi() {
        val url = ApiContract.RetenoApi.EventStatus
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(url = url)

        verify(inverse = true) {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_KEY),
                any()
            )
        }
        verify(inverse = true) {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_VERSION),
                any()
            )
        }
    }

    @Test
    fun addedPersistentHeaders() {
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        val method = HttpMethod.GET

        makeRequest(method)

        verify {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_ACCEPT),
                eq(RetenoRestClientProxy.HEADER_ACCEPT_VALUE)
            )
        }
        verify { httpURLConnection.readTimeout = eq(RetenoRestClientProxy.READ_TIMEOUT) }
        verify { httpURLConnection.connectTimeout = eq(RetenoRestClientProxy.TIMEOUT) }
        verify { httpURLConnection.requestMethod = method.httpMethodName }
        verify { httpURLConnection.useCaches = false }
        verify { httpURLConnection.instanceFollowRedirects = true }
    }

    @Test
    fun addedSslFactory_httpsRequest() {
        val httpsURLConnection = mockk<HttpsURLConnection>(relaxed = true)

        every { ConnectionManager.openConnection(any()) } returns httpsURLConnection
        val url = ApiContract.MobileApi.Events
        makeRequest(url = url)

        verify { httpsURLConnection.sslSocketFactory = any() }
    }

    @Test
    fun addedHeadersSpecificForPostAndPutMethods() {
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest()

        verify {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_CONTENT),
                eq(RetenoRestClientProxy.HEADER_CONTENT_VALUE)
            )
        }
        verify {
            httpURLConnection.setRequestProperty(
                eq(RetenoRestClientProxy.HEADER_ENCODING),
                eq(RetenoRestClientProxy.HEADER_ENCODING_VALUE)
            )
        }
        verify { httpURLConnection.doInput = true }
        verify { httpURLConnection.setChunkedStreamingMode(0) }
    }

    @Test
    fun attachedBody() {
        val body = "some body"
        val outputStream = mockk<OutputStream>(relaxed = true)

        every { httpURLConnection.getRequestProperty(eq(RetenoRestClientProxy.HEADER_CONTENT_ENCODING)) } returns ""
        every { httpURLConnection.outputStream } returns outputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(body = body)

        verify { outputStream.close() }
    }

    @Test
    fun success_makeRequest_httpOk() {
        val inputStream =
            ByteArrayInputStream(TEST_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_OK
        every { httpURLConnection.inputStream } returns inputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(responseCallback = spyCallback)

        verify { httpURLConnection.connect() }
        assertEquals(HttpURLConnection.HTTP_OK, httpURLConnection.responseCode)
        verify { httpURLConnection.inputStream }
        verify(inverse = true) { httpURLConnection.errorStream }
        verify { spyCallback.onSuccess(eq(TEST_RESPONSE)) }
        verify(inverse = true) { spyCallback.onFailure(any(), any(), any()) }
        verify { httpURLConnection.disconnect() }
    }

    @Test
    fun success_makeRequest_httpMovedPermanently() {
        val inputStream =
            ByteArrayInputStream(TEST_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_MOVED_PERM
        every { httpURLConnection.inputStream } returns inputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(responseCallback = spyCallback)

        verify { httpURLConnection.connect() }
        assertEquals(HttpURLConnection.HTTP_MOVED_PERM, httpURLConnection.responseCode)
        verify { httpURLConnection.inputStream }
        verify(inverse = true) { httpURLConnection.errorStream }
        verify { spyCallback.onSuccess(eq(TEST_RESPONSE)) }
        verify(inverse = true) { spyCallback.onFailure(any(), any(), any()) }
        verify { httpURLConnection.disconnect() }
    }

    @Test
    fun error_makeRequest_httpError() {
        val errorStream =
            ByteArrayInputStream(TEST_ERROR_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_BAD_REQUEST
        every { httpURLConnection.errorStream } returns errorStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        makeRequest(responseCallback = spyCallback)

        verify { httpURLConnection.connect() }
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, httpURLConnection.responseCode)
        verify { httpURLConnection.errorStream }
        verify(inverse = true) { spyCallback.onSuccess(any()) }
        verify {
            spyCallback.onFailure(
                eq(HttpURLConnection.HTTP_BAD_REQUEST),
                eq(TEST_ERROR_RESPONSE),
                null
            )
        }
        verify { httpURLConnection.disconnect() }
    }

    @Test
    fun exception_makeRequest_duringInitialisationHttpConnection() {
        val exceptionMessage = "test exception"
        val spyCallback = spyk<ResponseCallback>()
        every { ConnectionManager.openConnection(any()) } throws MockKException(exceptionMessage)

        makeRequest(responseCallback = spyCallback)

        verify { spyCallback.onFailure(null, null, any<MockKException>()) }
        verify(inverse = true) { spyCallback.onSuccess(any()) }
    }

    @Test
    fun exception_makeRequest_attachBody() {
        val exceptionMessage = "test exception"
        val spyCallback = spyk<ResponseCallback>()

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        every { httpURLConnection.getRequestProperty(eq(RetenoRestClientProxy.HEADER_CONTENT_ENCODING)) } returns ""
        every { httpURLConnection.outputStream } throws MockKException(exceptionMessage)

        makeRequest(responseCallback = spyCallback)

        verify { spyCallback.onFailure(null, null, any<MockKException>()) }
        verify { httpURLConnection.disconnect() }
        verify(inverse = true) { spyCallback.onSuccess(any()) }
    }

    private fun makeRequest(
        method: HttpMethod = HttpMethod.POST,
        url: ApiContract = ApiContract.Custom(TEST_URL),
        body: String? = null,
        queryParams: Map<String, Any>? = null,
        responseCallback: ResponseCallback = getCallback()
    ) {
        RetenoRestClientProxy.makeRequest(method, url, body, queryParams, responseCallback)
    }

    private fun generateUriWithParams(params: Map<String, Any>): String {
        var firstParam = true
        return buildString {
            append(TEST_URL)
            params.forEach { (key, value) ->
                if (firstParam) {
                    firstParam = false
                    append("?")
                } else {
                    append("&")
                }
                append("$key=$value")
            }
        }
    }

    private fun getCallback(
        success: ((String) -> Unit)? = null,
        error: ((Int?, String?, Throwable?) -> Unit)? = null
    ): ResponseCallback {
        return object : ResponseCallback {
            override fun onSuccess(response: String) {
                println(response)
                success?.invoke(response)
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                println(throwable?.message)
                error?.invoke(statusCode, response, throwable)
            }
        }

    }

}