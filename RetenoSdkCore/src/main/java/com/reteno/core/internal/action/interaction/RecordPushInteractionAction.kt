package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class RecordPushInteractionAction(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val initGate: InitGate,
    private val processCachedInteractions: ProcessCachedInteractions,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(interactionId: String, status: InteractionStatus): Result<Unit> = runner("RecordPushInteraction") {
        args("id" to interactionId, "status" to status)
        val request = InteractionRequestDb(id = interactionId, status = status.toDb())
        step("save interaction request to local database") { withContext(dispatchers.io) { interactionsDb.insertInteractionRequest(request) } }
        step("wait for sdk initialization") { initGate.awaitInit() }
        processCachedInteractions()
    }
}
