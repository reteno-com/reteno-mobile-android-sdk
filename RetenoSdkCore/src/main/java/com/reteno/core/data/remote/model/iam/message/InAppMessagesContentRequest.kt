package com.reteno.core.data.remote.model.iam.message

import com.google.gson.annotations.SerializedName

data class InAppMessagesContentRequest(
    @SerializedName("messageInstanceIds")
    val messageInstanceIds: List<Long>
)