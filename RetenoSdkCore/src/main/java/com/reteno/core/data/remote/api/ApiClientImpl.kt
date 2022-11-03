package com.reteno.core.data.remote.api

import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.ResponseCallback

internal class ApiClientImpl : ApiClient {

    override fun put(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.addOperation {
            putSync(url, jsonBody, responseHandler)
        }
    }

    override fun putSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.PUT, url, jsonBody, null, responseHandler)
    }

    override fun post(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.addOperation {
            postSync(url, jsonBody, responseHandler)
        }
    }

    override fun postSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.POST, url, jsonBody, null, responseHandler)
    }

    override fun get(url: ApiContract, queryParams: Map<String, Any>?, responseHandler: ResponseCallback
    ) {
        OperationQueue.addOperation {
            getSync(url, queryParams, responseHandler)
        }
    }

    override fun getSync(url: ApiContract, queryParams: Map<String, Any>?, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.GET, url, null, queryParams, responseHandler)
    }
}