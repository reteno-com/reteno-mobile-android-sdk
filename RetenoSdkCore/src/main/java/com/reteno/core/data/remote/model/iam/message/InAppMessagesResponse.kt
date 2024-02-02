package com.reteno.core.data.remote.model.iam.message

import com.google.gson.annotations.SerializedName

data class InAppMessagesResponse(
    @SerializedName("messages")
    val messages: List<InAppMessage>
)