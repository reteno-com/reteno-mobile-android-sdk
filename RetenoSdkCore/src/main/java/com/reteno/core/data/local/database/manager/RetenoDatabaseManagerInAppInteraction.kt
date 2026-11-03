package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InAppInteractionDb

interface RetenoDatabaseManagerInAppInteraction {
    fun insertInteraction(interaction: InAppInteractionDb)
    fun recordAttempt(interaction: InAppInteractionDb)
    fun deferUntil(interaction: InAppInteractionDb, nextAttemptAt: Long)
    fun getInteractions(limit: Int? = null): List<InAppInteractionDb>
    fun isWorkflowShown(context: String, iid: String): Boolean
    fun getPendingInteractions(context: String): List<InAppInteractionDb>
    fun getInAppInteractionsCount(): Long
    fun deleteInteraction(interaction: InAppInteractionDb): Boolean
    fun deleteInteractionsByTime(outdatedTime: String): List<InAppInteractionDb>
}