package com.reteno.core.data.local.model.recommendation

data class RecomEventDb(
    val rowId: String? = null,
    val recomEventType: RecomEventTypeDb,
    val occurred: String,
    val productId: String
)
