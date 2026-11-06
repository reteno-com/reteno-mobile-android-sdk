package com.reteno.core.data.remote.model.interaction

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.interaction.InteractionAction

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
    val statusDescription: String? = null,
    val serverAssignedIid: Boolean? = null,
    val action: InteractionAction? = null
)