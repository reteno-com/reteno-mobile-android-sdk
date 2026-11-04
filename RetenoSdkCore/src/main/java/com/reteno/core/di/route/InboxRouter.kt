package com.reteno.core.di.route

import com.reteno.core.di.graph.InboxGraph
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.internal.action.inbox.InboxActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class InboxRouter(
    private val inbox: InboxGraph,
    private val scope: CoroutineScope
) : InboxActions {

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        scope.launch { inbox.getInboxMessages(page, pageSize, status, callback) }
    }

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) {
        scope.launch { inbox.getInboxMessagesCount(callback) }
    }

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) {
        inbox.inboxCountPoller.subscribe(callback)
    }

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) {
        inbox.inboxCountPoller.unsubscribe(callback)
    }

    override fun unsubscribeAllInboxCount() {
        inbox.inboxCountPoller.unsubscribeAll()
    }

    override fun submitInboxMessageOpened(messageId: String) {
        scope.launch { inbox.markInboxMessageOpened(messageId) }
    }

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) {
        scope.launch { inbox.markAllInboxMessagesOpened(callback) }
    }
}
