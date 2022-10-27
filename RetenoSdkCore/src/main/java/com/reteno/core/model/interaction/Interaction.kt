package com.reteno.core.model.interaction

import com.google.gson.annotations.SerializedName

data class Interaction(
    @SerializedName("status")
    val status: InteractionStatus,
    @SerializedName("time")
    val time: String,
    @SerializedName("token")
    val token: String
)