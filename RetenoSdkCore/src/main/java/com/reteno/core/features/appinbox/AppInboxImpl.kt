package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog

internal class AppInboxImpl(
    private val actions: InboxActions,
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
            actions.submitInboxMessagesRequest(page, pageSize, status, callback)
        }
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        passToSdk("getAppInboxMessagesCount") {
            actions.submitInboxCountRequest(callback)
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("subscribeOnMessagesCountChanged") {
            actions.subscribeInboxCount(callback)
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        passToSdk("unsubscribeMessagesCountChanged") {
            actions.unsubscribeInboxCount(callback)
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        passToSdk("unsubscribeAllMessagesCountChanged") {
            actions.unsubscribeAllInboxCount()
        }
    }

    override fun markAsOpened(messageId: String) {
        passToSdk("markAsOpened") {
            actions.submitInboxMessageOpened(messageId)
        }
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        passToSdk("markAllMessagesAsOpened") {
            actions.submitAllInboxMessagesOpened(callback)
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
