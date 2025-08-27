package com.reteno.core.data.local.model.interaction

data class InteractionRequestDb(
    val rowId: String? = null,
    val id: String,
    val status: InteractionStatusDb
)