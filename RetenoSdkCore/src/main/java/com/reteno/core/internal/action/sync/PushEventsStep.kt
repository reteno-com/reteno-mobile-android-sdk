package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.model.ecom.RemoteConstants
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.util.Util.fromRemoteExplicitMillis
import kotlinx.coroutines.withContext

internal class PushEventsStep(
    private val eventsDb: RetenoDatabaseManagerEvents,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushEvents") { push() }

    private suspend fun ActionTrace.push(): StepOutcome {
        var budget = MAX_BATCHES
        while (budget > 0) {
            val groups = step("load events from local database", detail = { "groups=${it.size}" }) {
                withContext(dispatchers.io) { eventsDb.getEvents() }
            }
            if (groups.isEmpty()) return finishedAfter(MAX_BATCHES - budget)
            val batch = groups.take(budget)
            budget -= batch.size
            for (events in batch) {
                if (send(events) == StepOutcome.ABORT) return StepOutcome.ABORT
            }
        }
        return StepOutcome.CONTINUE
    }

    private suspend fun ActionTrace.send(events: EventsDb): StepOutcome {
        if (events.eventList.isNotEmpty()) {
            val result = step("send events to server", detail = { "count=${events.eventList.size}  $it" }) {
                api.request(HttpMethod.POST, ApiContract.MobileApi.Events, events.latestOfDistinctTypes().toRemote().toJson())
            }
            if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
        }
        step("delete sent events from local database") {
            withContext(dispatchers.io) { eventsDb.deleteEvents(events) }
        }
        return StepOutcome.CONTINUE
    }

    private fun EventsDb.latestOfDistinctTypes(): EventsDb = copy(
        eventList = eventList
            .groupBy { it.eventTypeKey }
            .flatMap { (type, events) ->
                if (type in DISTINCT_TYPES) listOf(events.maxBy { it.occurred.fromRemoteExplicitMillis() }) else events
            }
    )

    companion object {
        const val MAX_BATCHES = 50
        private val DISTINCT_TYPES = setOf(RemoteConstants.EcomEvent.EVENT_TYPE_CART_UPDATED)
    }
}
