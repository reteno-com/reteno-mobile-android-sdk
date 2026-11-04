package com.reteno.core.internal.action.inbox

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class MarkInboxMessageOpenedAction(
    private val inboxDb: RetenoDatabaseManagerAppInbox,
    private val configRepository: ConfigRepository,
    private val sync: SyncTrigger,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val timestamp: () -> String,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(messageId: String): Result<Unit> = runner("MarkInboxMessageOpened") {
        args("messageId" to messageId)
        step("wait for sdk initialization") {
            initGate.awaitUserInit()
        }
        step("save opened message to local database") {
            withContext(dispatchers.io) {
                val openedMessage = AppInboxMessageDb(
                    id = messageId,
                    deviceId = configRepository.getDeviceId().id,
                    occurredDate = timestamp()
                )
                inboxDb.insertAppInboxMessage(openedMessage)
            }
        }
        step("request data sync") {
            sync.request()
        }
    }
}
