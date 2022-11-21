package com.reteno.core.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages

class AppInboxImpl(
    private val appInboxController: AppInboxController
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        appInboxController.getAppInboxMessages(page, pageSize, callback)
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        appInboxController.getMessagesCount(callback)
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        appInboxController.subscribeCountChanges(callback)
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        appInboxController.unsubscribeCountChanges(callback)
    }

    override fun unsubscribeAllMessagesCountChanged() {
        appInboxController.unsubscribeAllCountChanges()
    }

    override fun markAsOpened(messageId: String) {
        appInboxController.markAsOpened(messageId)
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        appInboxController.markAllMessagesAsOpened(callback)
    }
}