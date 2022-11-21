package com.reteno.core.data.remote.model.interaction

import com.google.gson.annotations.SerializedName

data class InteractionRemote(
    @SerializedName("status")
    val status: InteractionStatusRemote,
    @SerializedName("time")
    val time: String,
    @SerializedName("token")
    val token: String
)