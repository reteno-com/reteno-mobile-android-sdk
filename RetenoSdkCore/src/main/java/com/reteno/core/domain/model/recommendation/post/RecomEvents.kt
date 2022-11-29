package com.reteno.core.domain.model.recommendation.post

/**
 * Model to log Recommendation events
 *
 * @param recomVariantId REQUIRED. Recommendation variant id. It is in r{recomId}v{variantId} format. Where recomId and variant Id are integer identifiers
 * @param recomEvents list of [com.reteno.core.domain.model.recommendation.post.RecomEvent]
 */
data class RecomEvents(
    val recomVariantId: String,
    val recomEvents: List<RecomEvent>?
)
