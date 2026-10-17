package com.reteno.core.internal.action.inbox

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE_SIZE
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_STATUS
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toDomain
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.SuspendApiClient

internal class GetInboxMessagesAction(
    private val api: SuspendApiClient,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ): Result<Unit> = runner("GetInboxMessages") {
        args("page" to page, "pageSize" to pageSize, "status" to status)
        step("wait for sdk initialization") {
            initGate.awaitUserInit()
        }
        val filters = mapOf(
            QUERY_PAGE to page?.toString(),
            QUERY_PAGE_SIZE to pageSize?.toString(),
            QUERY_STATUS to status?.str
        )
        val response = step("load inbox messages from server") {
            api.request(HttpMethod.GET, ApiContract.AppInbox.Messages, queryParams = filters)
        }
        passResponseToHost(response, callback, dispatchers) { it.fromJson<InboxMessagesRemote>().toDomain() }
    }
}
