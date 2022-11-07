package com.reteno.core.data.repository

import com.reteno.core.model.interaction.Interaction

interface InteractionRepository {

    fun saveInteraction(interactionId: String, interaction: Interaction)
    fun pushInteractions()
}