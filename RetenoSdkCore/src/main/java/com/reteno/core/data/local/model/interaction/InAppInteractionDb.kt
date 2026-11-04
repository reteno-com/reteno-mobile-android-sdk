package com.reteno.core.data.local.model.interaction


data class InAppInteractionDb(
    val rowId: String? = null,
    val interactionId: String,
    val time: String,
    val messageInstanceId: Long,
    val status: String,
    val statusDescription: String? = null,
    val deliveryContext: String? = null,
    val deliveryExpiresAt: String? = null,
    val delivered: Boolean = false,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0,
    val action: String? = null
)