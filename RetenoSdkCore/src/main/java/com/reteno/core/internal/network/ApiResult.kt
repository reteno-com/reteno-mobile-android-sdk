package com.reteno.core.internal.network

import com.reteno.core.util.isRepeatableError

internal sealed interface ApiResult {

    data class Success(
        val body: String,
        val headers: Map<String, List<String>>
    ) : ApiResult

    data class Failure(
        val statusCode: Int?,
        val body: String?,
        val error: Throwable?
    ) : ApiResult {
        val isRepeatable: Boolean
            get() = isRepeatableError(statusCode)
    }
}
