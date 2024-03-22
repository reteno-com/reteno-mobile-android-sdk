package com.reteno.core.data.remote.model.interaction

import com.google.gson.annotations.SerializedName

internal data class InAppInteractionRemote(
    @SerializedName("iid")
    val interactionId: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,
    @SerializedName("status")
    val status: String,
    @SerializedName("statusDescription")
    val statusDescription: String? = null
)