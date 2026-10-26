package com.reteno.core.internal.network

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class SuspendApiClient(
    private val restClient: RestClient,
    private val dispatchers: RetenoDispatchers
) {

    suspend fun request(
        method: HttpMethod,
        contract: ApiContract,
        body: String? = null,
        headers: Map<String, String>? = null,
        queryParams: Map<String, String?>? = null,
        retryCount: Int = 0
    ): ApiResult = withContext(dispatchers.io) {
        val callback = CapturingCallback()
        restClient.makeRequest(method, contract, body, headers, queryParams, retryCount, callback)
        callback.result ?: ApiResult.Failure(null, null, IllegalStateException(NO_CALLBACK))
    }

    private class CapturingCallback : ResponseCallback {
        var result: ApiResult? = null

        override fun onSuccess(headers: Map<String, List<String>>, response: String) {
            result = ApiResult.Success(response, headers)
        }

        override fun onSuccess(response: String) {
            result = ApiResult.Success(response, emptyMap())
        }

        override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            result = ApiResult.Failure(statusCode, response, throwable)
        }
    }

    private companion object {
        const val NO_CALLBACK = "RestClient returned without invoking the response callback"
    }
}
