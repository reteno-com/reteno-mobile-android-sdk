package com.reteno.core.features.iam

import com.google.gson.annotations.SerializedName

internal data class IamJsPayload(
    @SerializedName("targetComponentId")
    val targetComponentId: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("reason")
    val reason: String?,
    @SerializedName("contentHeight")
    val contentHeight: String?,
    @SerializedName("customData")
    val customData: HashMap<String, String>? = null
)
