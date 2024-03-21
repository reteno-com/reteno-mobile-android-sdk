package com.reteno.core.data.remote.model.iam.displayrules.async

data class AsyncRuleRetryParams(
    var statusCode: Int,
    var retryAfter: Long?
) {
    fun shouldRetry(sessionStartTimestamp: Long, lastRetryTime: Long): Boolean {
        return when (statusCode) {
            RETRY_BY_GIVEN_TIME -> {
                retryAfter?.let {
                    System.currentTimeMillis() - lastRetryTime > it
                } ?: true
            }
            RETRY_NEXT_SESSION, DO_NOT_RETRY_WITHOUT_MODIFICATION -> {
                lastRetryTime < sessionStartTimestamp
            }
            else -> System.currentTimeMillis() - lastRetryTime > STANDARD_RETRY_TIME
        }
    }

    companion object {
        const val STANDARD_RETRY_TIME = 5L * 60L * 1000L
        private const val DO_NOT_RETRY_WITHOUT_MODIFICATION = 422
        private const val RETRY_BY_GIVEN_TIME = 429
        private const val RETRY_NEXT_SESSION = 500
    }
}