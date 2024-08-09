package com.reteno.core.data

import android.net.Uri
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.ConnectionManager
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RestClientImpl
import com.reteno.core.domain.ResponseCallback
import io.mockk.MockKException
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.powermock.reflect.Whitebox
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

@RunWith(JUnit4::class)
class RestClientImplTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val TEST_URL = "http://www.test.com"
        private const val TEST_RESPONSE = "test response"
        private const val TEST_ERROR_RESPONSE = "error response"

        private val TIMEOUT: Int = getField("TIMEOUT")
        private val READ_TIMEOUT: Int = getField("READ_TIMEOUT")
        private val HEADER_KEY: String = getField("HEADER_KEY")
        private val HEADER_VERSION: String = getField("HEADER_VERSION")
        private val HEADER_CONTENT: String = getField("HEADER_CONTENT")
        private val HEADER_CONTENT_VALUE: String = getField("HEADER_CONTENT_VALUE")
        private val HEADER_ENCODING: String = getField("HEADER_ENCODING")
        private val HEADER_CONTENT_ENCODING: String = getField("HEADER_CONTENT_ENCODING")
        private val HEADER_ENCODING_VALUE: String = getField("HEADER_ENCODING_VALUE")
        private val HEADER_ACCEPT: String = getField("HEADER_ACCEPT")
        private val HEADER_ACCEPT_VALUE: String = getField("HEADER_ACCEPT_VALUE")

        private fun <T> getField(fieldName: String): T {
            return Whitebox.getField(
                RestClientImpl::class.java,
                fieldName
            )[RestClientImpl::class.java] as T
        }

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkObject(ConnectionManager)
            mockkStatic(Uri::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkObject(ConnectionManager)
            unmockkStatic(Uri::class)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var httpURLConnection: HttpsURLConnection
    private lateinit var restClient: RestClientImpl
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        restClient = RestClientImpl(
            RestConfig(
                mockk(relaxed = true),
                mockk(relaxed = true),
                "",
                initIdMode = DeviceIdMode.ANDROID_ID,
            ), platform = "Android"
        )
    }

    @Test
    fun appendedQueryParams_paramsNotNull() {
        // Given
        val expectedUrl = "http://www.test.com?params1=false&params2=9"
        val params = mapOf("params1" to false.toString(), "params2" to "9")

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        val mockUri = mockk<Uri>()
        val mockBuilder = mockk<Uri.Builder>()
        every { Uri.parse(any()) } returns mockUri
        every { mockUri.buildUpon() } returns mockBuilder
        every { mockUri.toString() } returns generateUriWithParams(params)
        every { mockBuilder.appendQueryParameter(any(), any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockUri

        // When
        makeRequest(HttpMethod.GET, queryParams = params)

        // Then
        verify { mockBuilder.appendQueryParameter(any(), any()) }
        verify { ConnectionManager.openConnection(eq(expectedUrl)) }
    }

    @Test
    fun appendedQueryParams_paramsAreEmpty() {
        // Given
        val params = emptyMap<String, String?>()
        val expectedUrl = "http://www.test.com"

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(HttpMethod.GET, queryParams = params)

        // Then
        verify(inverse = true) { Uri.parse(any()) }
        verify { ConnectionManager.openConnection(eq(expectedUrl)) }
    }

    @Test
    fun addedAuthorizationHeaders_callMobileApi() {
        // Given
        val url = ApiContract.MobileApi.Events
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(url = url)

        // Then
        verify { httpURLConnection.setRequestProperty(eq(HEADER_KEY), any()) }
        verify {
            httpURLConnection.setRequestProperty(
                eq(HEADER_VERSION),
                any()
            )
        }
    }

    @Test
    fun didntAddedAuthorizationHeaders_callIsNotMobileApi() {
        // Given
        val url = ApiContract.RetenoApi.InteractionStatus("")
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(url = url)

        // Then
        verify(inverse = true) {
            httpURLConnection.setRequestProperty(
                eq(HEADER_KEY),
                any()
            )
        }
        verify(inverse = true) {
            httpURLConnection.setRequestProperty(
                eq(HEADER_VERSION),
                any()
            )
        }
    }

    @Test
    fun addedPersistentHeaders() {
        // Given
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        val method = HttpMethod.GET

        // When
        makeRequest(method)

        // Then
        verify {
            httpURLConnection.setRequestProperty(
                eq(HEADER_ACCEPT),
                eq(HEADER_ACCEPT_VALUE)
            )
        }
        verify { httpURLConnection.readTimeout = eq(READ_TIMEOUT) }
        verify { httpURLConnection.connectTimeout = eq(TIMEOUT) }
        verify { httpURLConnection.requestMethod = method.httpMethodName }
        verify { httpURLConnection.useCaches = false }
        verify { httpURLConnection.instanceFollowRedirects = true }
    }

    @Test
    fun addedSslFactory_httpsRequest() {
        // Given
        val httpsURLConnection = mockk<HttpsURLConnection>(relaxed = true)

        every { ConnectionManager.openConnection(any()) } returns httpsURLConnection
        val url = ApiContract.MobileApi.Events

        // When
        makeRequest(url = url)

        // Then
        verify { httpsURLConnection.sslSocketFactory = any() }
    }

    @Test
    fun addedHeadersSpecificForPostAndPutMethods() {
        // Given
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest()

        // Then
        verify {
            httpURLConnection.setRequestProperty(
                eq(HEADER_CONTENT),
                eq(HEADER_CONTENT_VALUE)
            )
        }
        verify {
            httpURLConnection.setRequestProperty(
                eq(HEADER_ENCODING),
                eq(HEADER_ENCODING_VALUE)
            )
        }
        verify { httpURLConnection.doInput = true }
        verify { httpURLConnection.setChunkedStreamingMode(0) }
    }

    @Test
    fun attachedBody() {
        // Given
        val body = "some body"
        val outputStream = mockk<OutputStream>(relaxed = true)

        every { httpURLConnection.getRequestProperty(eq(HEADER_CONTENT_ENCODING)) } returns ""
        every { httpURLConnection.outputStream } returns outputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(body = body)

        // Then
        verify { outputStream.close() }
    }

    @Test
    fun success_makeRequest_httpOk() {
        // Given
        val inputStream = ByteArrayInputStream(TEST_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()
        every {
            spyCallback.onSuccess(
                any(),
                any()
            )
        } answers { spyCallback.onSuccess(secondArg<String>()) }

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_OK
        every { httpURLConnection.inputStream } returns inputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(responseCallback = spyCallback)

        // Then
        verify { httpURLConnection.connect() }
        assertEquals(HttpURLConnection.HTTP_OK, httpURLConnection.responseCode)
        verify { httpURLConnection.inputStream }
        verify(inverse = true) { httpURLConnection.errorStream }
        verify { spyCallback.onSuccess(any(), eq(TEST_RESPONSE)) }
        verify { spyCallback.onSuccess(eq(TEST_RESPONSE)) }
        verify(inverse = true) { spyCallback.onFailure(any(), any(), any()) }
        verify { httpURLConnection.disconnect() }
    }

    @Test
    fun success_makeRequest_httpMovedPermanently() {
        // Given
        val inputStream = ByteArrayInputStream(TEST_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()
        every {
            spyCallback.onSuccess(
                any(),
                any()
            )
        } answers { spyCallback.onSuccess(secondArg<String>()) }

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_MOVED_PERM
        every { httpURLConnection.inputStream } returns inputStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(responseCallback = spyCallback)

        // Then
        verify { httpURLConnection.connect() }
        assertEquals(HttpURLConnection.HTTP_MOVED_PERM, httpURLConnection.responseCode)
        verify { httpURLConnection.inputStream }
        verify(inverse = true) { httpURLConnection.errorStream }
        verify { spyCallback.onSuccess(any(), eq(TEST_RESPONSE)) }
        verify { spyCallback.onSuccess(eq(TEST_RESPONSE)) }
        verify(inverse = true) { spyCallback.onFailure(any(), any(), any()) }
        verify { httpURLConnection.disconnect() }
    }

    @Test
    fun error_makeRequest_httpError() {
        // Given
        val errorStream = ByteArrayInputStream(TEST_ERROR_RESPONSE.toByteArray())
        val spyCallback = spyk<ResponseCallback>()

        every { httpURLConnection.responseCode } returns HttpURLConnection.HTTP_BAD_REQUEST
        every { httpURLConnection.errorStream } returns errorStream
        every { ConnectionManager.openConnection(any()) } returns httpURLConnection

        // When
        makeRequest(responseCallback = spyCallback)

        // Then
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
        // Given
        val exceptionMessage = "test exception"
        val spyCallback = spyk<ResponseCallback>()
        every { ConnectionManager.openConnection(any()) } throws MockKException(exceptionMessage)

        // When
        makeRequest(responseCallback = spyCallback)

        // Then
        verify { spyCallback.onFailure(null, null, any<MockKException>()) }
        verify(inverse = true) { spyCallback.onSuccess(any()) }
    }

    @Test
    fun exception_makeRequest_attachBody() {
        // Given
        val exceptionMessage = "test exception"
        val spyCallback = spyk<ResponseCallback>()

        every { ConnectionManager.openConnection(any()) } returns httpURLConnection
        every { httpURLConnection.getRequestProperty(eq(HEADER_CONTENT_ENCODING)) } returns ""
        every { httpURLConnection.outputStream } throws MockKException(exceptionMessage)

        // When
        makeRequest(responseCallback = spyCallback)

        // Then
        verify { spyCallback.onFailure(null, null, any<MockKException>()) }
        verify { httpURLConnection.disconnect() }
        verify(inverse = true) { spyCallback.onSuccess(any()) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun makeRequest(
        method: HttpMethod = HttpMethod.POST,
        url: ApiContract = ApiContract.Custom(TEST_URL),
        body: String? = null,
        queryParams: Map<String, String?>? = null,
        responseCallback: ResponseCallback = getCallback()
    ) {
        restClient.makeRequest(method, url, body, null, queryParams, retryCount = 0, responseCallback)
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
    // endregion helper methods --------------------------------------------------------------------
}