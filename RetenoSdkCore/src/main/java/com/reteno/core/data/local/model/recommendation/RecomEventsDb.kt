package com.reteno.core.data.local.model.recommendation

data class RecomEventsDb(
    val recomVariantId: String,
    val recomEvents: List<RecomEventDb>?
)
