package com.reteno.core.data.local.model.interaction

data class InteractionDb(
    val interactionId: String,
    val status: InteractionStatusDb,
    val time: String,
    val token: String
)