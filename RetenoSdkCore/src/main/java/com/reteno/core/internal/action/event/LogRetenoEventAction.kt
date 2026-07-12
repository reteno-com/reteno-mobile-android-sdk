package com.reteno.core.internal.action.event

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.action.runAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LogRetenoEventAction(
    private val serviceLocator: ServiceLocator,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun invoke(event: RetenoLogEvent) = runAction(TAG, "LogRetenoEvent") {
        val databaseManager = serviceLocator.retenoDatabaseManagerLogEventProvider.get()
        val logEventDb = event.toDb()

        withContext(ioDispatcher) {
            databaseManager.insertLogEvent(logEventDb)
        }
    }

    companion object {
        private const val TAG = "LogRetenoEventAction"
    }
}