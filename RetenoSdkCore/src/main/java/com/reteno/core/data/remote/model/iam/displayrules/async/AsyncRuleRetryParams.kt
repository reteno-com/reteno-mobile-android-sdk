package com.reteno.core.data.remote.model.iam.displayrules.async

data class AsyncRuleRetryParams(
    var statusCode: Int,
    var retryAfter: Long?
)
