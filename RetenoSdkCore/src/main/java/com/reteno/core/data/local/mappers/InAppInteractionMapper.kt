package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.data.remote.mapper.toJson

fun InAppInteraction.toDb() = InAppInteractionDb(
    interactionId = interactionId,
    time = time.formatToRemote(),
    messageInstanceId = messageInstanceId,
    status = status.name,
    statusDescription = statusDescription,
    workflowContext = workflowContext,
    workflowExpiresAt = workflowExpiresAt,
    action = action?.toJson()
)