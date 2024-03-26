package com.reteno.core.data.local.model.interaction


data class InAppInteractionDb(
    val rowId: String? = null,
    val interactionId: String,
    val time: String,
    val messageInstanceId: Long,
    val status: String,
    val statusDescription: String? = null
)