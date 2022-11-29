package com.reteno.core.domain.model.recommendation.post

data class RecomEvents(
    val recomVariantId: String,
    val impressions: List<RecomEvent>?,
    val clicks: List<RecomEvent>?
)
