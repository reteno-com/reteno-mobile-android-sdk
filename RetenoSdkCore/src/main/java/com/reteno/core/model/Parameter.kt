package com.reteno.core.model

import com.google.gson.annotations.SerializedName

data class Parameter(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String
)
