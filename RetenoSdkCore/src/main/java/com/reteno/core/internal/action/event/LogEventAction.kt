package com.reteno.core.internal.action.event

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.runAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LogEventAction(
    private val serviceLocator: ServiceLocator,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun invoke(event: Event) = runAction(TAG, "LogEvent") {
        val currentRestConfig = serviceLocator.restConfigProvider.get()
        val databaseManager = serviceLocator.retenoDatabaseManagerEventsProvider.get()
        val events = EventsDb(
            deviceId = currentRestConfig.deviceId,
            eventDb = event.toDb()
        )

        withContext(ioDispatcher) {
            databaseManager.insertEvents(events)
        }
        eventFlow.emit(event)
    }

    companion object {
        private const val TAG = "LogEventAction"
    }
}