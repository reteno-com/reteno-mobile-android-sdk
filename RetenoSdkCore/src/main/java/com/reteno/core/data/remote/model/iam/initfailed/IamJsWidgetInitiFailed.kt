package com.reteno.core.data.remote.model.iam.initfailed

import com.google.gson.annotations.SerializedName

data class IamJsWidgetInitiFailed(
    @SerializedName("tenantId")
    val tenantId: String,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("log_level")
    val log_level: String = "ERROR",
    @SerializedName("message")
    val message: String = "IN_APP_ANDROID",
    @SerializedName("scriptVersion")
    val scriptVersion: String = "latest"
)