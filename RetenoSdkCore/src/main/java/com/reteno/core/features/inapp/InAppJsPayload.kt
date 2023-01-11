package com.reteno.core.features.inapp

import com.google.gson.annotations.SerializedName

internal data class InAppJsPayload(
    @SerializedName("targetComponentId")
    val targetComponentId: String?,
    @SerializedName("url")
    val url: String?
)
