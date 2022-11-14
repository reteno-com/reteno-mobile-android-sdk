package com.reteno.core.data.repository

import com.reteno.core.domain.model.interaction.Interaction
import java.time.ZonedDateTime

interface InteractionRepository {

    fun saveInteraction(interactionId: String, interaction: Interaction)
    fun pushInteractions()
    fun clearOldInteractions(outdatedTime: ZonedDateTime)
}