package com.reteno.core.data.repository

import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction

interface InteractionRepository {
    fun sendInteraction(interactionId: String, interaction: Interaction, responseHandler: ResponseCallback)
}