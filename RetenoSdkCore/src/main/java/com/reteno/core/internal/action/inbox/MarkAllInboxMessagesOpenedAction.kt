package com.reteno.core.internal.action.inbox

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.appinbox.AppInboxMessagesStatusRemote
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class MarkAllInboxMessagesOpenedAction(
    private val api: SuspendApiClient,
    private val inboxDb: RetenoDatabaseManagerAppInbox,
    private val inboxCount: InboxCountPolling,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(callback: RetenoResultCallback<Unit>): Result<Unit> = runner("MarkAllInboxMessagesOpened") {
        step("wait for sdk initialization") { initGate.awaitInit() }
        val response = step("send all messages opened to server") {
            api.request(HttpMethod.POST, ApiContract.AppInbox.MessagesStatus, AppInboxMessagesStatusRemote().toJson())
        }
        if (response is ApiResult.Success) {
            step("delete opened statuses from local database") { withContext(dispatchers.io) { inboxDb.deleteAllAppInboxMessages() } }
            step("refresh unread messages count") { inboxCount.refresh() }
        }
        passResponseToHost(response, callback, dispatchers) { }
    }
}
