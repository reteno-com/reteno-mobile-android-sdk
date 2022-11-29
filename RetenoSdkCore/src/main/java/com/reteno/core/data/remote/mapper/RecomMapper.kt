package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.remote.model.recommendation.get.RecomFilterRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomRequestRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRemote
import com.reteno.core.data.remote.model.recommendation.post.RecomEventsRequestRemote
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest

//-------------- Get -------------------------------------------------------------------------------
fun RecomRequest.toRemote() = RecomRequestRemote(
    products = products,
    category = category,
    fields = fields,
    filters = filters?.toRemote()
)

fun RecomFilter.toRemote() = RecomFilterRemote(
    name = name,
    values = values
)

//-------------- Post ------------------------------------------------------------------------------
fun List<RecomEventsDb>.toRemote() = RecomEventsRequestRemote(
    events = this.map { it.toRemote() }
)

fun RecomEventsDb.toRemote() = RecomEventsRemote(
    recomVariantId = recomVariantId,
    impressions = impressions?.map { it.toRemote() },
    clicks = clicks?.map { it.toRemote() }
)

fun RecomEventDb.toRemote() = RecomEventRemote(
    occurred = occurred,
    productId = productId
)