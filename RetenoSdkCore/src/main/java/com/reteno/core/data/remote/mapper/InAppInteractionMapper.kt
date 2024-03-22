package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.remote.model.interaction.InAppInteractionRemote

internal fun InAppInteractionDb.toRemote(): InAppInteractionRemote = InAppInteractionRemote(
    interactionId = interactionId,
    time = time,
    messageInstanceId = messageInstanceId,
    status = status,
    statusDescription = statusDescription
)