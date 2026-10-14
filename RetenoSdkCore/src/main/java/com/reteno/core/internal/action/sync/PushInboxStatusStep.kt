package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.appinbox.AppInboxMessagesStatusRemote
import com.reteno.core.internal.action.inbox.InboxCountPolling
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class PushInboxStatusStep(
    private val inboxDb: RetenoDatabaseManagerAppInbox,
    private val api: SuspendApiClient,
    private val inboxCount: InboxCountPolling,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushInboxStatus") { push() }

    private suspend fun ActionTrace.push(): StepOutcome {
        val messages = step("load opened inbox messages from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { inboxDb.getAppInboxMessages() }
        }
        if (messages.isEmpty()) return nothingToSend()
        val result = step("send opened status to server") {
            api.request(HttpMethod.POST, ApiContract.AppInbox.MessagesStatus, AppInboxMessagesStatusRemote(ids = messages.map { it.id }).toJson())
        }
        if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
        step("delete sent statuses from local database") {
            withContext(dispatchers.io) { inboxDb.deleteAppInboxMessages(messages) }
        }
        if (result is ApiResult.Success) {
            step("refresh unread messages count") { inboxCount.refresh() }
        }
        return StepOutcome.CONTINUE
    }
}
