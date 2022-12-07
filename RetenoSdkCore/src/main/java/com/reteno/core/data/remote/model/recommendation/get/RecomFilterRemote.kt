package com.reteno.core.data.remote.model.recommendation.get

import com.google.gson.annotations.SerializedName

internal data class RecomFilterRemote(
    @SerializedName("name")
    val name: String,
    @SerializedName("values")
    val values: List<String>
)