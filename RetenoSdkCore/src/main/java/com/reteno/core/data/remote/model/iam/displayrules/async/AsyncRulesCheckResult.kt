package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckResult(
    @SerializedName("segmentId")
    val segmentId: Long = 0,
    @SerializedName("checkResult")
    val checkResult: Boolean? = null,
    @SerializedName("error")
    val error: AsyncRulesCheckError? = null
)