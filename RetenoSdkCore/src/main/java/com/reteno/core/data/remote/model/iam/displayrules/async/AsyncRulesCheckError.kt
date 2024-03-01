package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckError(
    @SerializedName("status")
    val statusCode: Long,
    @SerializedName("retryAfter")
    val retryAfter: AsyncRulesCheckRetryModel
)