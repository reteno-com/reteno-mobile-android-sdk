package com.reteno.core.data.remote.api

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.ResponseCallback
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class ApiClientImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val JSON_BODY = "Json_body"
        private val QUERY_PARAMS = mapOf<String, String?>("Key" to "value", "key2" to null)
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    private lateinit var SUT: ApiClientImpl

    @RelaxedMockK
    private lateinit var restClient: RestClient
    @RelaxedMockK
    private lateinit var url: ApiContract
    @RelaxedMockK
    private lateinit var responseHandler: ResponseCallback
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = ApiClientImpl(restClient)
    }

    @Test
    fun whenPutCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.put(url, JSON_BODY, responseHandler)

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.PUT),
                apiContract = eq(url),
                body = eq(JSON_BODY),
                queryParams = null,
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun whenPutSyncCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.putSync(url, JSON_BODY, responseHandler)

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.PUT),
                apiContract = eq(url),
                body = eq(JSON_BODY),
                queryParams = null,
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun whenPostCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.post(url, JSON_BODY, responseHandler)

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.POST),
                apiContract = eq(url),
                body = eq(JSON_BODY),
                queryParams = null,
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun whenPostSyncCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.postSync(url, JSON_BODY, retryCount = 0, responseHandler)

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.POST),
                apiContract = eq(url),
                body = eq(JSON_BODY),
                queryParams = null,
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun given_whenGetCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.get(url, QUERY_PARAMS, responseHandler)

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.GET),
                apiContract = eq(url),
                body = null,
                queryParams = eq(QUERY_PARAMS),
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun whenGetSyncCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.getSync(url, QUERY_PARAMS, responseHandler)

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.GET),
                apiContract = eq(url),
                body = null,
                queryParams = eq(QUERY_PARAMS),
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun whenHeadCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.head(url, QUERY_PARAMS, responseHandler)

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.HEAD),
                apiContract = eq(url),
                body = null,
                queryParams = eq(QUERY_PARAMS),
                responseCallback = eq(responseHandler)
            )
        }
    }

    @Test
    fun given_whenHeadSyncCalled_thenMakeRequestCalledWithCorrectParams() {
        // When
        SUT.headSync(url, QUERY_PARAMS, responseHandler)

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) {
            restClient.makeRequest(
                method = eq(HttpMethod.HEAD),
                apiContract = eq(url),
                body = null,
                queryParams = eq(QUERY_PARAMS),
                responseCallback = eq(responseHandler)
            )
        }
    }
}