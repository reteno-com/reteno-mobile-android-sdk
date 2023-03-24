package com.reteno.core.data.remote.model.iam.initfailed

import com.google.gson.annotations.SerializedName
import com.reteno.core.data.remote.api.ApiContract

data class IamJsWidgetInitiFailed(
    @SerializedName("scriptVersion")
    val scriptVersion: String = "latest",
    @SerializedName("orgId")
    val orgId: Int = 0,
    @SerializedName("siteId")
    val siteId: Int = 0,
    @SerializedName("tenantId")
    val tenantId: String,
    @SerializedName("guid")
    val guid: String = "null",
    val url: String = ApiContract.InAppMessages.BaseHtml.url,
    @SerializedName("message")
    val message: String = "IN_APP_ANDROID",
    @SerializedName("log_level")
    val log_level: String = "ERROR",
    @SerializedName("data")
    val `data`: String,
)