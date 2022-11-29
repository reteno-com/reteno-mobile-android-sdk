package com.reteno.core.data.remote.model.recommendation.post

import com.google.gson.annotations.SerializedName

data class RecomEventRemote(
    @SerializedName("occurred")
    val occurred: String,
    @SerializedName("productId")
    val productId: String
)
