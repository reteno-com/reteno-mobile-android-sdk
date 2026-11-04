package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.fromDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface ProcessCachedInteractions {
    suspend operator fun invoke(): Result<Unit>
}

internal class ProcessCachedInteractionsAction(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val recordInteraction: RecordInteraction,
    private val sync: SyncTrigger,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : ProcessCachedInteractions {

    private val lock = Mutex()

    override suspend operator fun invoke(): Result<Unit> = runner("ProcessCachedInteractions") {
        lock.withLock { process() }
    }

    private suspend fun ActionTrace.process() {
        val requests = step<List<InteractionRequestDb>>(
            "load interaction requests from local database",
            detail = { "count=${it.size}" }
        ) {
            withContext(dispatchers.io) { interactionsDb.getInteractionRequests() }
        }
        if (requests.isEmpty()) {
            note { "no cached interactions" }
            return
        }
        val recorded = requests.count { record(it) }
        if (recorded > 0) {
            step("request data sync") {
                sync.request()
            }
        }
    }

    private suspend fun ActionTrace.record(request: InteractionRequestDb): Boolean {
        val recorded = recordInteraction(request.id, request.status.fromDb()).getOrDefault(false)
        if (!recorded) {
            note { "interaction kept for next start" }
            return false
        }
        step("delete interaction request from local database") {
            withContext(dispatchers.io) { interactionsDb.deleteInteractionRequest(request) }
        }
        return true
    }
}
