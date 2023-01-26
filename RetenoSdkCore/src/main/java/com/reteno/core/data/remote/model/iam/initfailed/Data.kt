package com.reteno.core.data.remote.model.iam.initfailed

import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("payload")
    val payload: Payload?,
    @SerializedName("type")
    val type: String
)