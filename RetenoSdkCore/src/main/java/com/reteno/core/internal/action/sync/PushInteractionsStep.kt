package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class PushInteractionsStep(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushInteractions") { push() }

    private suspend fun ActionTrace.push(): StepOutcome {
        var handled = 0
        while (true) {
            val interaction = step("load interaction from local database") {
                withContext(dispatchers.io) { interactionsDb.getInteractions(1).firstOrNull() }
            } ?: return finishedAfter(handled)
            val result = step("send interaction status to server") {
                api.request(
                    HttpMethod.PUT,
                    ApiContract.RetenoApi.InteractionStatus(interaction.interactionId),
                    interaction.toRemote().toJson()
                )
            }
            if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
            val deleted = step("delete sent interaction from local database") {
                withContext(dispatchers.io) { interactionsDb.deleteInteraction(interaction) }
            }
            if (!deleted) return StepOutcome.CONTINUE
            handled++
        }
    }
}
