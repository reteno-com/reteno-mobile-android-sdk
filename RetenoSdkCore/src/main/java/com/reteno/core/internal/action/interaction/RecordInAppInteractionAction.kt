package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class RecordInAppInteractionAction(
    private val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction,
    private val sync: SyncTrigger,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(interaction: InAppInteraction): Result<Unit> = runner("RecordInAppInteraction") {
        args(
            "id" to interaction.interactionId,
            "messageInstanceId" to interaction.messageInstanceId,
            "status" to interaction.status
        )
        step("save in-app interaction to local database") {
            withContext(dispatchers.io) { inAppInteractionsDb.insertInteraction(interaction.toDb()) }
        }
        step("request data sync") {
            sync.request()
        }
    }
}
