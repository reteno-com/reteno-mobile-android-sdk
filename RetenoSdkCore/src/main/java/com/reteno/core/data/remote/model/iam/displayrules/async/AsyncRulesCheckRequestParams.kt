package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckRequestParams(
    @SerializedName("segmentId")
    val segmentId: Long
)