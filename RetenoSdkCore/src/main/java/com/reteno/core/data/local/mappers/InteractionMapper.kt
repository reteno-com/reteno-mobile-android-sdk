package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus

internal fun Interaction.toDb(interactionId: String) = InteractionDb(
    interactionId = interactionId,
    status = status.toDb(),
    time = time,
    token = token
)

internal fun InteractionStatus.toDb(): InteractionStatusDb =
    when (this) {
        InteractionStatus.DELIVERED -> InteractionStatusDb.DELIVERED
        InteractionStatus.OPENED -> InteractionStatusDb.OPENED
    }