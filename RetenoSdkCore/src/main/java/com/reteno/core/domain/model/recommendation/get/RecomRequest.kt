package com.reteno.core.domain.model.recommendation.get

data class RecomRequest @JvmOverloads constructor(
    val products: List<String>?,
    val category: String?,
    val fields: List<String>? = null,
    val filters: RecomFilter? = null
)
