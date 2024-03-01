package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckResult(
    @SerializedName("segmentId")
    val segmentId: Long,
    @SerializedName("checkResult")
    val checkResult: Boolean,
    @SerializedName("error")
    val error: AsyncRulesCheckError
)