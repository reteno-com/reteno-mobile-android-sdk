package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckResponse (
    @SerializedName("checks")
    val checks: List<AsyncRulesCheckResult>
)