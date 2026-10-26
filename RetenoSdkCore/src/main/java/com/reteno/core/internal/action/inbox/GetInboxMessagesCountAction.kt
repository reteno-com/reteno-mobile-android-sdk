package com.reteno.core.internal.action.inbox

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.inbox.InboxMessagesCountRemote
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.SuspendApiClient

internal interface GetInboxMessagesCount {
    suspend operator fun invoke(callback: RetenoResultCallback<Int>): Result<Unit>
}

internal class GetInboxMessagesCountAction(
    private val api: SuspendApiClient,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : GetInboxMessagesCount {

    override suspend operator fun invoke(
        callback: RetenoResultCallback<Int>
    ): Result<Unit> = runner("GetInboxMessagesCount") {
        step("wait for sdk initialization") {
            initGate.awaitUserInit()
        }
        val response = step("load unread messages count from server") {
            api.request(HttpMethod.GET, ApiContract.AppInbox.MessagesCount)
        }
        passResponseToHost(response, callback, dispatchers) { it.fromJson<InboxMessagesCountRemote>().unreadCount }
    }
}
