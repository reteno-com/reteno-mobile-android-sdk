package com.reteno.core.data.repository

import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.Interaction

interface InteractionRepository {

    suspend fun saveInteraction(interactionId: String, interaction: Interaction)

    fun saveAndPushInAppInteraction(inAppInteraction: InAppInteraction)
}