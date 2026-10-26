package com.reteno.core.fixtures

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.internal.action.inbox.InboxActions

internal class FakeInboxActions(private val log: FakeCallLog = FakeCallLog()) : InboxActions {
    val callbacks = mutableListOf<Any>()

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        log.record("submitInboxMessagesRequest $page $pageSize $status")
        callbacks += callback
    }

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) {
        log.record("submitInboxCountRequest")
        callbacks += callback
    }

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) {
        log.record("subscribeInboxCount")
        callbacks += callback
    }

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) {
        log.record("unsubscribeInboxCount")
        callbacks += callback
    }

    override fun unsubscribeAllInboxCount() {
        log.record("unsubscribeAllInboxCount")
    }

    override fun submitInboxMessageOpened(messageId: String) {
        log.record("submitInboxMessageOpened $messageId")
    }

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) {
        log.record("submitAllInboxMessagesOpened")
        callbacks += callback
    }
}
