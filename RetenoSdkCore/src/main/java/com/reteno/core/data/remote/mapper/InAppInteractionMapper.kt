package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.remote.model.interaction.InAppInteractionRemote
import com.reteno.core.domain.model.interaction.InteractionAction

internal fun InAppInteractionDb.toRemote(): InAppInteractionRemote = InAppInteractionRemote(
    interactionId = interactionId,
    time = time,
    messageInstanceId = messageInstanceId,
    status = status,
    statusDescription = statusDescription,
    inWorkflow = if (workflowContext != null) true else null,
    action = action?.fromJson<InteractionAction>()
)