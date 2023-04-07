package com.reteno.core.data.remote.model.iam.initfailed

import com.google.gson.annotations.SerializedName

data class Payload(
    @SerializedName("reason")
    val reason: String?
)