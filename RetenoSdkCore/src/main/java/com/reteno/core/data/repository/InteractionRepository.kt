package com.reteno.core.data.repository

import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.Interaction
import java.time.ZonedDateTime

interface InteractionRepository {

    suspend fun saveInteraction(interactionId: String, interaction: Interaction)
    fun pushInteractions()
    fun clearOldInteractions(outdatedTime: ZonedDateTime)

    fun saveAndPushInAppInteraction(inAppInteraction: InAppInteraction)
    fun pushInAppInteractions()
    fun clearOldInAppInteractions(outdatedTime: ZonedDateTime)
}