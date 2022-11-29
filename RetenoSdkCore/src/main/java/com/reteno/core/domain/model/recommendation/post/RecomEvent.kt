package com.reteno.core.domain.model.recommendation.post

import java.time.ZonedDateTime

/**
 * Recommendation event
 *
 * @property recomEventType REQUIRED. Can be either IMPRESSIONS or CLICKS
 * @property occurred REQUIRED. Time in [java.time.ZonedDateTime]. Usually
 * ```
 * ZoneDateTime.now()
 * ```
 * @property productId REQUIRED. Id of the product
 *
 * @see [com.reteno.core.domain.model.recommendation.post.RecomEventType]
 */
data class RecomEvent(
    val recomEventType: RecomEventType,
    val occurred: ZonedDateTime,
    val productId: String
)
