package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.domain.model.recommendation.post.RecomEvent
import com.reteno.core.domain.model.recommendation.post.RecomEventType
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.util.Util.formatToRemote

internal fun RecomEvents.toDb() = RecomEventsDb(
    recomVariantId = recomVariantId,
    recomEvents = recomEvents?.map { it.toDb() }
)

internal fun RecomEvent.toDb() = RecomEventDb(
    recomEventType = recomEventType.toDb(),
    occurred = occurred.formatToRemote(),
    productId = productId
)

internal fun RecomEventType.toDb() =
    when (this) {
        RecomEventType.CLICKS -> RecomEventTypeDb.CLICKS
        RecomEventType.IMPRESSIONS -> RecomEventTypeDb.IMPRESSIONS
    }