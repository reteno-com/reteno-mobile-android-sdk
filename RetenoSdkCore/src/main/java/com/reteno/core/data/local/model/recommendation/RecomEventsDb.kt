package com.reteno.core.data.local.model.recommendation

data class RecomEventsDb(
    val recomVariantId: String,
    val impressions: List<RecomEventDb>?,
    val clicks: List<RecomEventDb>?
)
