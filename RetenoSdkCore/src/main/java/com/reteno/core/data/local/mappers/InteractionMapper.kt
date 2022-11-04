package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.InteractionModelDb
import com.reteno.core.model.interaction.Interaction


fun Interaction.toDb(interactionId: String) = InteractionModelDb(
    interactionId = interactionId,
    status = status,
    time = time,
    token = token
)