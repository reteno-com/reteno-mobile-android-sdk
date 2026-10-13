package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.logevent.RetenoLogEventListRemote
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class PushLogEventsStep(
    private val logEventsDb: RetenoDatabaseManagerLogEvent,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushLogEvents", reportFailure = false) {
        dropBacklogAboveLimit()
        push()
    }

    private suspend fun ActionTrace.dropBacklogAboveLimit() {
        val count = step("count log events in local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) { logEventsDb.getLogEventsCount() }
        }
        var excess = count - MAX_BACKLOG
        while (excess > 0) {
            val chunk = minOf(excess, DELETE_CHUNK.toLong()).toInt()
            val deleted = step("delete oldest log events above limit from local database", detail = { "count=$chunk" }) {
                withContext(dispatchers.io) { logEventsDb.deleteLogEvents(logEventsDb.getLogEvents(chunk)) }
            }
            if (!deleted) return
            excess -= chunk
        }
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        var handled = 0
        while (true) {
            val batch = step("load log events from local database", detail = { "count=${it.size}" }) {
                withContext(dispatchers.io) { logEventsDb.getLogEvents(BATCH_SIZE) }
            }
            if (batch.isEmpty()) return finishedAfter(handled)
            val result = step("send log events to server") {
                api.request(HttpMethod.POST, ApiContract.LogEvent.Events, RetenoLogEventListRemote(batch.map { it.toUploadableRemote() }).toJson())
            }
            if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
            val deleted = step("delete sent log events from local database") {
                withContext(dispatchers.io) { logEventsDb.deleteLogEvents(batch) }
            }
            if (!deleted) return StepOutcome.CONTINUE
            handled++
        }
    }

    private fun RetenoLogEventDb.toUploadableRemote() = toRemote().let { remote ->
        remote.copy(errorMessage = remote.errorMessage?.let(Redact::uploadable))
    }

    companion object {
        const val DELETE_CHUNK = 200
        const val BATCH_SIZE = 50
        const val MAX_BACKLOG = 500
    }
}
