package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckRetryModel(
    @SerializedName("unit")
    val timeUnit: String,
    @SerializedName("amount")
    val amount: Long
)