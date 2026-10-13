package com.reteno.core.data.remote.api

import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.ResponseCallback

internal class ApiClientImpl(private val restClient: RestClient) : ApiClient {

    override fun post(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        postWithRetry(url, jsonBody, 0, responseHandler)
    }

    override fun postWithRetry(
        url: ApiContract,
        jsonBody: String,
        retryCount: Int,
        responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            restClient.makeRequest(HttpMethod.POST, url, jsonBody, null, null, retryCount, responseHandler)
        }
    }

    override fun get(
        url: ApiContract,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            restClient.makeRequest(HttpMethod.GET, url, null, headers, queryParams, responseCallback = responseHandler)
        }
    }

    override fun get(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        get(url, null, queryParams, responseHandler)
    }

    override fun head(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            restClient.makeRequest(HttpMethod.HEAD, url, null, null, queryParams, responseCallback = responseHandler)
        }
    }
}
