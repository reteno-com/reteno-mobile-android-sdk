package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog

internal class AppInboxImpl(
    private val inboxActions: InboxActions,
    private val remoteLog: RemoteLog
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        passToSdk("getAppInboxMessages") {
            inboxActions.submitInboxMessagesRequest(page, pageSize, status, callback)
        }
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        passToSdk("getAppInboxMessagesCount") {
            inboxActions.submitInboxCountRequest(callback)
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("subscribeOnMessagesCountChanged") {
            inboxActions.subscribeInboxCount(callback)
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("unsubscribeMessagesCountChanged") {
            inboxActions.unsubscribeInboxCount(callback)
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        passToSdk("unsubscribeAllMessagesCountChanged") {
            inboxActions.unsubscribeAllInboxCount()
        }
    }

    override fun markAsOpened(messageId: String) {
        passToSdk("markAsOpened") {
            inboxActions.submitInboxMessageOpened(messageId)
        }
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        passToSdk("markAllMessagesAsOpened") {
            inboxActions.submitAllInboxMessagesOpened(callback)
        }
    }

    private fun passToSdk(call: String, submit: () -> Unit) {
        try {
            submit()
        } catch (error: Throwable) {
            remoteLog.error("AppInbox.$call failed: ${Redact.summary(error)}")
        }
    }
}
