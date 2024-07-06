package com.reteno.core.data.remote.api

import com.reteno.core.domain.ResponseCallback

internal interface RestClient {

    /**
     *  Perform Http request with
     *
     *  @param method HttpMethod type
     *  @param url base url + endpoint wrapped in ApiContract
     *  @param body JSON body for POST and PUT methods
     *  @param queryParams params for GET method
     *  @param responseCallback
     *
     *  @see com.reteno.data.remote.api.HttpMethod
     *  @see com.reteno.data.remote.api.ApiContract
     *  @see com.reteno.domain.ResponseCallback
     */
    fun makeRequest(
        method: HttpMethod,
        apiContract: ApiContract,
        body: String?,
        headers: Map<String, String>? = null,
        queryParams: Map<String, String?>? = null,
        retryCount:Int = 0,
        responseCallback: ResponseCallback
    )
}