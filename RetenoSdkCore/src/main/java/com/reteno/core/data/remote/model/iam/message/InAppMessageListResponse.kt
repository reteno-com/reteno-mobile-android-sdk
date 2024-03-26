package com.reteno.core.data.remote.model.iam.message

import com.google.gson.annotations.SerializedName

data class InAppMessageListResponse(
    @SerializedName("messages")
    val messages: List<InAppMessageResponse>
)