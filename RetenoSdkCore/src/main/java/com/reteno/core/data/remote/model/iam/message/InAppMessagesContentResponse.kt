package com.reteno.core.data.remote.model.iam.message

import com.google.gson.annotations.SerializedName

data class InAppMessagesContentResponse(
    @SerializedName("contents")
    val contents: List<InAppMessageContent>
)