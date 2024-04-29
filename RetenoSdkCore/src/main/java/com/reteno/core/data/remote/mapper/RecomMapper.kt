package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.remote.model.recommendation.get.RecomFilterRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomRequestRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRequestRemote
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest

//-------------- Get -------------------------------------------------------------------------------
internal fun RecomRequest.toRemote() = RecomRequestRemote(
    products = products,
    category = category,
    fields = fields,
    filters = filters?.map { it.toRemote() }
)

internal fun RecomFilter.toRemote() = RecomFilterRemote(
    name = name,
    values = values
)

//-------------- Post ------------------------------------------------------------------------------
internal fun List<RecomEventsDb>.toRemote() = RecomEventsRequestRemote(
    events = this.map { it.toRemote() }
)

internal fun RecomEventsDb.toRemote() = RecomEventsRemote(
    recomVariantId = recomVariantId,
    impressions = recomEvents
        ?.filter { it.recomEventType == RecomEventTypeDb.IMPRESSIONS }
        ?.map { it.toRemote() },
    clicks = recomEvents
        ?.filter { it.recomEventType == RecomEventTypeDb.CLICKS }
        ?.map { it.toRemote() },
)

internal fun RecomEventDb.toRemote() = RecomEventRemote(
    occurred = occurred,
    productId = productId
)