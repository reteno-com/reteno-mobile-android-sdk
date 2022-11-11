package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.model.interaction.InteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote

fun InteractionDb.toRemote() = InteractionRemote(
    status = status.toRemote(),
    time = time,
    token = token
)

fun InteractionStatusDb.toRemote(): InteractionStatusRemote =
    when (this) {
        InteractionStatusDb.DELIVERED -> InteractionStatusRemote.DELIVERED
        InteractionStatusDb.OPENED -> InteractionStatusRemote.OPENED
    }