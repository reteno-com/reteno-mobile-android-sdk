package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InAppInteractionDb

interface RetenoDatabaseManagerInAppInteraction {
    fun insertInteraction(interaction: InAppInteractionDb)
    fun getInteractions(limit: Int? = null): List<InAppInteractionDb>
    fun getInAppInteractionsCount(): Long
    fun deleteInteraction(interaction: InAppInteractionDb): Boolean
    fun deleteInteractionsByTime(outdatedTime: String): List<InAppInteractionDb>
}