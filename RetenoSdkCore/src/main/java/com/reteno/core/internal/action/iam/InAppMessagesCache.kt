package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class InAppMessagesCache(
    private val inAppDb: RetenoDatabaseManagerInAppMessages,
    private val dispatchers: RetenoDispatchers
) {

    private val writes = Mutex()

    suspend fun load(): List<InAppMessageDb> = withContext(dispatchers.io) { inAppDb.getInAppMessages() }

    suspend fun replaceAll(messages: List<InAppMessage>) = writes.withLock {
        withContext(dispatchers.io) {
            inAppDb.deleteAllInAppMessages()
            inAppDb.insertInAppMessages(messages.map { it.toDB() })
        }
    }

    suspend fun update(messages: List<InAppMessage>) = writes.withLock {
        withContext(dispatchers.io) {
            val rows = messages.map { it.toDB() }
            inAppDb.deleteInAppMessages(rows)
            inAppDb.insertInAppMessages(rows)
        }
    }
}
