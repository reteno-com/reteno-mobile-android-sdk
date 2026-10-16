package com.reteno.core.internal.network

import com.reteno.core.internal.action.trace.TracedOutcome
import com.reteno.core.util.isRepeatableError

internal sealed interface ApiResult {

    data class Success(
        val body: String,
        val headers: Map<String, List<String>>
    ) : ApiResult, TracedOutcome {
        override val traceLabel: String
            get() = "ok"

        override val isSuccessful: Boolean
            get() = true
    }

    data class Failure(
        val statusCode: Int?,
        val body: String?,
        val error: Throwable?
    ) : ApiResult, TracedOutcome {
        val isRepeatable: Boolean
            get() = isRepeatableError(statusCode)

        val isNotModified: Boolean
            get() = statusCode == HTTP_NOT_MODIFIED

        override val traceLabel: String
            get() = when {
                isNotModified -> "$HTTP_NOT_MODIFIED not modified"
                else -> "${statusCode ?: "no response"} ${if (isRepeatable) "repeatable" else "non-repeatable"}"
            }

        override val isSuccessful: Boolean
            get() = isNotModified
    }
}

private const val HTTP_NOT_MODIFIED = 304
