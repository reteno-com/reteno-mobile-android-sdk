package com.reteno.core.data.remote.api

import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.ResponseCallback

internal class ApiClientImpl(private val restClient: RestClient) : ApiClient {

    override fun put(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.addOperation {
            putSync(url, jsonBody, responseHandler)
        }
    }

    override fun putSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        restClient.makeRequest(HttpMethod.PUT, url, jsonBody, null, null,  responseHandler)
    }

    override fun post(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.addOperation {
            postSync(url, jsonBody, responseHandler)
        }
    }

    override fun postSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        restClient.makeRequest(HttpMethod.POST, url, jsonBody, null, null, responseHandler)
    }

    override fun get(
        url: ApiContract,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            getSync(url, headers, queryParams, responseHandler)
        }
    }

    override fun get(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        get(url, null, queryParams, responseHandler)
    }

    override fun getSync(
        url: ApiContract,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        restClient.makeRequest(HttpMethod.GET, url, null,  headers, queryParams, responseHandler)
    }

    override fun getSync(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        getSync(url, null, queryParams, responseHandler)
    }

    override fun head(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            headSync(url, queryParams, responseHandler)
        }
    }

    override fun headSync(
        url: ApiContract,
        queryParams: Map<String, String?>?,
        responseHandler: ResponseCallback
    ) {
        restClient.makeRequest(HttpMethod.HEAD, url, null, null,  queryParams, responseHandler)
    }
}