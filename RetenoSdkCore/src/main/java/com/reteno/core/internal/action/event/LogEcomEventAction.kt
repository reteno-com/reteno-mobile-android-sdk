package com.reteno.core.internal.action.event

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.asRegularEvent
import com.reteno.core.internal.action.runAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LogEcomEventAction(
    private val serviceLocator: ServiceLocator,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun invoke(event: EcomEvent) = runAction(TAG, "LogEcomEvent") {
        val currentRestConfig = serviceLocator.restConfigProvider.get()
        val databaseManager = serviceLocator.retenoDatabaseManagerEventsProvider.get()
        val events = EventsDb(
            deviceId = currentRestConfig.deviceId,
            eventDb = event.toDb()
        )

        withContext(ioDispatcher) {
            databaseManager.insertEvents(events)
        }
        eventFlow.emit(event.asRegularEvent())
    }

    companion object {
        private const val TAG = "LogEcomEventAction"
    }
}