package com.reteno.core.data.remote.api

import com.reteno.core.domain.ResponseCallback

internal interface ApiClient {

    fun put(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback)
    fun putSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback)

    fun post(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback)
    fun postSync(url: ApiContract, jsonBody: String, responseHandler: ResponseCallback)

    fun get(url: ApiContract, queryParams: Map<String, String?>?, responseHandler: ResponseCallback)
    fun getSync(url: ApiContract, queryParams: Map<String, String?>?, responseHandler: ResponseCallback)
}