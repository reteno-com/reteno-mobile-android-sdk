package com.reteno.core.internal.action.event

import com.reteno.core.data.local.config.DeviceIdentity
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.asRegularEvent
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.event.EventBus
import kotlinx.coroutines.withContext

internal class LogEcomEventAction(
    private val eventsDb: RetenoDatabaseManagerEvents,
    private val restConfig: DeviceIdentity,
    private val eventBus: EventBus,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(event: EcomEvent): Result<Unit> = runner("LogEcomEvent") {
        args("type" to event.typeKey)
        step("save ecommerce event to local database") {
            withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) }
        }
        step("notify in-app message triggers") {
            eventBus.emit(event.asRegularEvent())
        }
    }
}
