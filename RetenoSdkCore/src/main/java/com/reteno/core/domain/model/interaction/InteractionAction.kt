package com.reteno.core.domain.model.interaction

import com.google.gson.annotations.SerializedName

data class InteractionAction(
    val type: String,
    @SerializedName("targetComponentId")
    val targetComponentId: String?,
    @SerializedName("url")
    val url: String?,
)