package com.reteno.core.domain.model.recommendation.post

import java.time.ZonedDateTime

data class RecomEvent(
    val recomEventType: RecomEventType,
    val occurred: ZonedDateTime,
    val productId: String
)
