package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InteractionDb

interface RetenoDatabaseManagerInteraction {
    fun insertInteraction(interaction: InteractionDb)
    fun getInteractions(limit: Int? = null): List<InteractionDb>
    fun getInteractionCount(): Long
    fun deleteInteraction(interaction: InteractionDb): Boolean
    fun deleteInteractionByTime(outdatedTime: String): List<InteractionDb>
}