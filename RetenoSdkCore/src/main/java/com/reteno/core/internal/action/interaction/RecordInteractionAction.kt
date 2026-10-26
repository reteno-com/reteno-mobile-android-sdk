package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.Redact
import kotlinx.coroutines.withContext

internal interface RecordInteraction {
    suspend operator fun invoke(interactionId: String, status: InteractionStatus): Result<Boolean>
}

internal class RecordInteractionAction(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val configRepository: ConfigRepository,
    private val dispatchers: RetenoDispatchers,
    private val timestamp: () -> String,
    private val runner: ActionRunner
) : RecordInteraction {

    override suspend operator fun invoke(
        interactionId: String,
        status: InteractionStatus
    ): Result<Boolean> = runner("RecordInteraction") {
        args("id" to interactionId, "status" to status)
        record(interactionId, status)
    }

    private suspend fun ActionTrace.record(interactionId: String, status: InteractionStatus): Boolean {
        val token = step("read push token", detail = { Redact.token(it) }) {
            withContext(dispatchers.io) { configRepository.getFcmToken() }
        }
        if (token.isBlank()) {
            note { "no push token yet, interaction not recorded" }
            return false
        }
        val interaction = Interaction(status, timestamp(), token = token)
        step("save interaction to local database") {
            withContext(dispatchers.io) { interactionsDb.insertInteraction(interaction.toDb(interactionId)) }
        }
        return true
    }
}
