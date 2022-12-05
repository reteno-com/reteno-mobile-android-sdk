package com.reteno.core.data.remote.model.appinbox

import com.google.gson.annotations.SerializedName

data class AppInboxMessagesStatusRemote(
    @SerializedName("status")
    val status: String = "OPENED",
    @SerializedName("ids")
    val ids: List<String>? = null
)
