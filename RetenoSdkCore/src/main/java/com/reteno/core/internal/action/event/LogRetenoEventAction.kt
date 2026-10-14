package com.reteno.core.internal.action.event

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class LogRetenoEventAction(
    private val logEventsDb: RetenoDatabaseManagerLogEvent,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(event: RetenoLogEvent): Result<Unit> = runner("LogRetenoEvent", reportFailure = false) {
        args("level" to event.logLevel)
        step("save log event to local database") {
            withContext(dispatchers.io) { logEventsDb.insertLogEvent(event.toDb()) }
        }
    }
}
