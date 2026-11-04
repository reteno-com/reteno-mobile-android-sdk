package com.reteno.core.fixtures

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.SuspendApiClient
import java.io.IOException

internal data class SentRequest(
    val method: HttpMethod,
    val contract: ApiContract,
    val body: String?,
    val queryParams: Map<String, String?>? = null,
    val headers: Map<String, String>? = null,
    val retryCount: Int = 0
)

internal class FakeRestClient : RestClient {
    var context = "test-context"
    val requests = mutableListOf<SentRequest>()

    override fun workflowContext(): String = context

    override fun makeWorkflowRequest(
        context: String,
        method: HttpMethod,
        url: ApiContract,
        body: String,
        responseCallback: ResponseCallback
    ) {
        if (context != this.context) {
            responseCallback.onFailure(null, null, IllegalStateException("Workflow identity changed"))
            return
        }
        makeRequest(method, url, body, null, null, 0, responseCallback)
    }

    private val responses = ArrayDeque<(ResponseCallback) -> Unit>()
    private val responsesByAddress = mutableMapOf<String, ArrayDeque<(ResponseCallback) -> Unit>>()

    fun succeedWith(contract: ApiContract, body: String) {
        responsesByAddress.getOrPut(contract.url) { ArrayDeque() }.addLast { it.onSuccess(emptyMap(), body) }
    }

    fun succeed(times: Int = 1) = repeat(times) { responses.addLast { it.onSuccess(emptyMap(), "") } }

    fun succeedWith(body: String) = responses.addLast { it.onSuccess(emptyMap(), body) }

    fun succeedWith(body: String, headers: Map<String, List<String>>) =
        responses.addLast { it.onSuccess(headers, body) }

    fun failWith(statusCode: Int, body: String) = responses.addLast { it.onFailure(statusCode, body, null) }

    fun failWith(statusCode: Int, times: Int = 1) = repeat(times) {
        responses.addLast { it.onFailure(statusCode, null, null) }
    }

    fun failOffline() = responses.addLast { it.onFailure(null, null, IOException("offline")) }

    fun failWith(error: Throwable) = responses.addLast { it.onFailure(null, null, error) }

    override fun makeRequest(
        method: HttpMethod,
        apiContract: ApiContract,
        body: String?,
        headers: Map<String, String>?,
        queryParams: Map<String, String?>?,
        retryCount: Int,
        responseCallback: ResponseCallback
    ) {
        requests += SentRequest(method, apiContract, body, queryParams, headers, retryCount)
        val respond = responsesByAddress[apiContract.url]?.removeFirstOrNull()
            ?: responses.removeFirstOrNull()
            ?: { callback: ResponseCallback -> callback.onSuccess(emptyMap(), "") }
        respond(responseCallback)
    }
}

internal fun FakeRestClient.api(dispatchers: RetenoDispatchers) = SuspendApiClient(this, dispatchers)
