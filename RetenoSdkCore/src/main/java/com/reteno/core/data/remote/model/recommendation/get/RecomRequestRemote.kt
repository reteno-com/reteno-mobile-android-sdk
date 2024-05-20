package com.reteno.core.data.remote.model.recommendation.get

import com.google.gson.annotations.SerializedName

internal data class RecomRequestRemote @JvmOverloads constructor(
    @SerializedName("products")
    val products: List<String>?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("fields")
    val fields: List<String>? = null,
    @SerializedName("filters")
    val filters: List<RecomFilterRemote>? = null
)
