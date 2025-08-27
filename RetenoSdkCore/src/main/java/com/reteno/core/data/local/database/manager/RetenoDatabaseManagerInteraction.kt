package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb

interface RetenoDatabaseManagerInteraction {
    fun insertInteractionRequest(request: InteractionRequestDb)
    fun getInteractionRequests(): List<InteractionRequestDb>
    fun deleteInteractionRequest(interaction: InteractionRequestDb): Boolean
    fun insertInteraction(interaction: InteractionDb)
    fun getInteractions(limit: Int? = null): List<InteractionDb>
    fun getInteractionCount(): Long
    fun deleteInteraction(interaction: InteractionDb): Boolean
    fun deleteInteractionByTime(outdatedTime: String): List<InteractionDb>
}