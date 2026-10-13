package com.reteno.core.internal.action.event

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.event.EventBus
import kotlinx.coroutines.withContext

internal class LogEventAction(
    private val eventsDb: RetenoDatabaseManagerEvents,
    private val deviceId: () -> DeviceId,
    private val eventBus: EventBus,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(event: Event): Result<Unit> = runner("LogEvent") {
        args("type" to event.eventTypeKey)
        step("save event to local database") {
            withContext(dispatchers.io) {
                eventsDb.insertEvents(EventsDb(deviceId(), event.toDb()))
            }
        }
        step("notify in-app message triggers") { eventBus.emit(event) }
    }
}
