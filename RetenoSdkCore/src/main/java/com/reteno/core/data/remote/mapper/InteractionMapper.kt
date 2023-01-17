package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.model.interaction.InteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote

internal fun InteractionDb.toRemote() = InteractionRemote(
    status = status.toRemote(),
    time = time,
    token = token
)

internal fun InteractionStatusDb.toRemote(): InteractionStatusRemote =
    when (this) {
        InteractionStatusDb.DELIVERED -> InteractionStatusRemote.DELIVERED
        InteractionStatusDb.CLICKED -> InteractionStatusRemote.CLICKED
        InteractionStatusDb.OPENED -> InteractionStatusRemote.OPENED
    }