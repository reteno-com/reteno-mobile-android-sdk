package com.reteno.core.data.remote.ds

import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction

interface InteractionRepository {
    fun sendInteraction(interactionId: String, interaction: Interaction, responseHandler: ResponseCallback)
}