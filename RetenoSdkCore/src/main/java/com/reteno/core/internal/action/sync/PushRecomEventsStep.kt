package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
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

internal class PushRecomEventsStep(
    private val recomEventsDb: RetenoDatabaseManagerRecomEvents,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override val name: String = "PushRecomEvents"

    override suspend fun invoke(): Result<StepOutcome> = runner(name) {
        push()
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        val groups = step("load recommendation events from local database", detail = { "groups=${it.size}" }) {
            withContext(dispatchers.io) { recomEventsDb.getRecomEvents() }
        }
        if (groups.isEmpty()) return nothingToSend()
        if (groups.all { it.recomEvents.isNullOrEmpty() }) {
            step("delete empty recommendation groups from local database") {
                withContext(dispatchers.io) { recomEventsDb.deleteRecomEvents(groups) }
            }
            return StepOutcome.CONTINUE
        }
        val result = step("send recommendation events to server") {
            api.request(HttpMethod.POST, ApiContract.Recommendation.PostRecoms, groups.toRemote().toJson())
        }
        if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
        step("delete sent recommendation events from local database") {
            withContext(dispatchers.io) { recomEventsDb.deleteRecomEvents(groups) }
        }
        return StepOutcome.CONTINUE
    }
}
