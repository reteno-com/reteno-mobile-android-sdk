package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class LogRecommendationEventsAction(
    private val recomEventsDb: RetenoDatabaseManagerRecomEvents,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(events: RecomEvents): Result<Unit> = runner("LogRecommendationEvents") {
        save(events)
    }

    private suspend fun ActionTrace.save(events: RecomEvents) {
        val recomEvents = events.recomEvents.orEmpty()
        args("recomVariantId" to events.recomVariantId, "events" to recomEvents.size)
        if (recomEvents.isEmpty()) {
            note { "no recommendation events, nothing to save" }
            return
        }
        step("save recommendation events to local database") {
            withContext(dispatchers.io) { recomEventsDb.insertRecomEvents(events.toDb()) }
        }
    }
}
