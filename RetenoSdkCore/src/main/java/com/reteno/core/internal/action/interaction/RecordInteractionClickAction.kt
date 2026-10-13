package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class RecordInteractionClickAction(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val sync: SyncTrigger,
    private val dispatchers: RetenoDispatchers,
    private val timestamp: () -> String,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(interactionId: String, action: InteractionAction, forcePush: Boolean): Result<Unit> = runner("RecordInteractionClick") {
        args("id" to interactionId, "action" to action.type, "forcePush" to forcePush)
        val interaction = Interaction(InteractionStatus.CLICKED, timestamp(), action = action)
        step("save clicked interaction to local database") { withContext(dispatchers.io) { interactionsDb.insertInteraction(interaction.toDb(interactionId)) } }
        if (forcePush) step("request data sync") { sync.request() }
    }
}
