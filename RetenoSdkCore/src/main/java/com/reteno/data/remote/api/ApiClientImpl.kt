package com.reteno.data.remote.api

import com.reteno.data.remote.OperationQueue
import com.reteno.domain.ResponseCallback

internal class ApiClientImpl : ApiClient {

    override fun put(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.getInstance().addOperation {
            putSync(url, jsonBody, responseHandler)
        }
    }

    override fun putSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.PUT, url, jsonBody, null,responseHandler)
    }

    override fun post(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        OperationQueue.getInstance().addOperation {
            postSync(url, jsonBody, responseHandler)
        }
    }

    override fun postSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.POST, url, jsonBody, null, responseHandler)
    }

    override fun get(url: ApiContract, queryParams: Map<String, Any>?, responseHandler: ResponseCallback
    ) {
        OperationQueue.getInstance().addOperation {
            getSync(url, queryParams, responseHandler)
        }
    }

    override fun getSync(url: ApiContract, queryParams: Map<String, Any>?, responseHandler: ResponseCallback) {
        RetenoRestClient.makeRequest(HttpMethod.GET, url, null, queryParams, responseHandler)
    }
}