package com.reteno.core.data.remote.model.iam.message

import com.google.gson.annotations.SerializedName

data class InAppMessage(
    @SerializedName("messageId")
    val messageId: Long,
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,
//    @SerializedName("messageId")
//    val displayRules: String,
)