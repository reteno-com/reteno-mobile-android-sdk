package com.reteno.core.domain.model.interaction

data class Interaction(
    val status: InteractionStatus,
    val time: String,
    val token: String
)