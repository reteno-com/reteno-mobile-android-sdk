package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.model.interaction.InteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus

fun Interaction.toRemote() = InteractionRemote(
    status = status.toRemote(),
    time = time,
    token = token
)

fun InteractionStatus.toRemote(): InteractionStatusRemote =
    when (this) {
        InteractionStatus.DELIVERED -> InteractionStatusRemote.DELIVERED
        InteractionStatus.OPENED -> InteractionStatusRemote.OPENED
    }

//==================================================================================================
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