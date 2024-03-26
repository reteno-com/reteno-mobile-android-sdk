package com.reteno.core.data.remote.model.iam.message

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class InAppMessageContent(
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,

    @SerializedName("layoutType")
    val layoutType: String,

    @SerializedName("model")
    val model: JsonElement
)