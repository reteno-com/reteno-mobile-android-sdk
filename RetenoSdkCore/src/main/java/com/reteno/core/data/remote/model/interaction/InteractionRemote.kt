package com.reteno.core.data.remote.model.interaction

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.interaction.InteractionAction

internal data class InteractionRemote(
    @SerializedName("status")
    val status: InteractionStatusRemote,
    @SerializedName("time")
    val time: String,
    @SerializedName("token")
    val token: String?,
    @SerializedName("action")
    val action: InteractionAction? = null,
)