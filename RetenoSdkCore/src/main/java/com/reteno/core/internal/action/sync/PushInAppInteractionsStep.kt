package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
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

internal class PushInAppInteractionsStep(
    private val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override val name: String = "PushInAppInteractions"

    override suspend fun invoke(): Result<StepOutcome> = runner(name) {
        push()
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        var handled = 0
        while (true) {
            val interaction = step("load in-app interaction from local database") {
                withContext(dispatchers.io) { inAppInteractionsDb.getInteractions(1).firstOrNull() }
            } ?: return finishedAfter(handled)
            val result = step("send in-app interaction to server") {
                api.request(
                    HttpMethod.POST,
                    ApiContract.InAppMessages.RegisterInteraction,
                    interaction.toRemote().toJson()
                )
            }
            if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
            val deleted = step("delete sent in-app interaction from local database") {
                withContext(dispatchers.io) { inAppInteractionsDb.deleteInteraction(interaction) }
            }
            if (!deleted) return StepOutcome.CONTINUE
            handled++
        }
    }
}
