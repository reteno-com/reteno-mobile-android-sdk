package com.reteno.core.data.remote.model.recommendation.post

import com.google.gson.annotations.SerializedName

data class RecomEventsRemote(
    @SerializedName("recomVariantId")
    val recomVariantId: String,
    @SerializedName("impressions")
    val impressions: List<RecomEventRemote>?,
    @SerializedName("clicks")
    val clicks: List<RecomEventRemote>?
)
