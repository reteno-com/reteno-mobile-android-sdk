package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.remote.model.interaction.InAppInteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.internal.iam.serverAssignedIid

internal fun InAppInteractionDb.toRemote(): InAppInteractionRemote = InAppInteractionRemote(
    interactionId = interactionId,
    time = time,
    messageInstanceId = messageInstanceId,
    status = status,
    statusDescription = statusDescription,
    serverAssignedIid = if (serverAssignedIid) true else null,
    action = action?.fromJson<InteractionAction>()
)

internal fun InAppInteractionDb.toClickRemote(): InteractionRemote = InteractionRemote(
    status = InteractionStatusRemote.CLICKED,
    time = time,
    token = null,
    action = action?.fromJson<InteractionAction>()
)
