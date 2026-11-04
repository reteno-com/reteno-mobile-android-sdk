package com.reteno.core.data.remote.model.iam.message

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class InAppMessageResponse(
    @SerializedName("messageId")
    val messageId: Long,
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,
    @SerializedName("displayRules")
    val displayRules: JsonObject,
    val iid: String? = null,
    val expiresAt: String? = null,
    val priority: Int? = null,
)