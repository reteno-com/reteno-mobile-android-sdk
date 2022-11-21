package com.reteno.core.data.remote.model.event

import com.google.gson.annotations.SerializedName

data class ParameterRemote(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String?
)
