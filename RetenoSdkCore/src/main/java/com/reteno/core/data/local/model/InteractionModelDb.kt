package com.reteno.core.data.local.model

import com.google.gson.annotations.SerializedName
import com.reteno.core.model.interaction.InteractionStatus

data class InteractionModelDb(
    @SerializedName("interactionId")
    val interactionId: String,
    @SerializedName("status")
    val status: InteractionStatus,
    @SerializedName("time")
    val time: String,
    @SerializedName("token")
    val token: String
)