package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog

internal class AppInboxImpl(
    private val inboxActions: Lazy<InboxActions>,
    private val remoteLog: RemoteLog,
    private val isOsSupported: () -> Boolean
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        passToSdk("getAppInboxMessages") {
            inboxActions.value.submitInboxMessagesRequest(page, pageSize, status, callback)
        }
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        passToSdk("getAppInboxMessagesCount") {
            inboxActions.value.submitInboxCountRequest(callback)
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("subscribeOnMessagesCountChanged") {
            inboxActions.value.subscribeInboxCount(callback)
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("unsubscribeMessagesCountChanged") {
            inboxActions.value.unsubscribeInboxCount(callback)
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        passToSdk("unsubscribeAllMessagesCountChanged") {
            inboxActions.value.unsubscribeAllInboxCount()
        }
    }

    override fun markAsOpened(messageId: String) {
        passToSdk("markAsOpened") {
            inboxActions.value.submitInboxMessageOpened(messageId)
        }
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        passToSdk("markAllMessagesAsOpened") {
            inboxActions.value.submitAllInboxMessagesOpened(callback)
        }
    }

    private fun passToSdk(call: String, submit: () -> Unit) {
        if (!isOsSupported()) return
        try {
            submit()
        } catch (error: Throwable) {
            remoteLog.error("AppInbox.$call failed: ${Redact.summary(error)}")
        }
    }
}
