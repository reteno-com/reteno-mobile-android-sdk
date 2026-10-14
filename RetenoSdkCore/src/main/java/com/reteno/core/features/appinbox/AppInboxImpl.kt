package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.internal.action.inbox.InboxActions

internal class AppInboxImpl(
    private val actions: InboxActions,
    private val isOsSupported: () -> Boolean
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        if (!isOsSupported()) return
        actions.submitInboxMessagesRequest(page, pageSize, status, callback)
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        if (!isOsSupported()) return
        actions.submitInboxCountRequest(callback)
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        if (!isOsSupported()) return
        actions.subscribeInboxCount(callback)
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        if (!isOsSupported()) return
        actions.unsubscribeInboxCount(callback)
    }

    override fun unsubscribeAllMessagesCountChanged() {
        if (!isOsSupported()) return
        actions.unsubscribeAllInboxCount()
    }

    override fun markAsOpened(messageId: String) {
        if (!isOsSupported()) return
        actions.submitInboxMessageOpened(messageId)
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        if (!isOsSupported()) return
        actions.submitAllInboxMessagesOpened(callback)
    }
}
