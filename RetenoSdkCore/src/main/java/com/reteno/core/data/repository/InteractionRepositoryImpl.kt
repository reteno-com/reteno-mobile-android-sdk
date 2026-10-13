package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.util.Logger

internal class InteractionRepositoryImpl(
    private val interactionDatabaseManager: RetenoDatabaseManagerInteraction,
    private val inAppInteractionDatabaseManager: RetenoDatabaseManagerInAppInteraction,
    private val requestSync: () -> Unit
) : InteractionRepository {

    override suspend fun saveInteraction(interactionId: String, interaction: Interaction) {
        /*@formatter:off*/ Logger.i(TAG, "saveInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction , "]")
        /*@formatter:on*/
        interactionDatabaseManager.insertInteraction(interaction.toDb(interactionId))
    }

    override fun saveAndPushInAppInteraction(inAppInteraction: InAppInteraction) {
        /*@formatter:off*/ Logger.i(TAG, "saveInAppInteraction(): ", "inAppInteraction = [" , inAppInteraction , "]")
        /*@formatter:on*/
        OperationQueue.addParallelOperation {
            inAppInteractionDatabaseManager.insertInteraction(inAppInteraction.toDb())
            requestSync()
        }
    }

    companion object {
        private val TAG = InteractionRepositoryImpl::class.java.simpleName

        private const val REMOVE_INTERACTIONS = "Removed interactions"
        private const val REMOVE_IN_APP_INTERACTIONS = "Removed inAppInteractions"
    }
}