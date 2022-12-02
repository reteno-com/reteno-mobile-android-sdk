package com.reteno.core.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.util.Logger

class AppInboxImpl(
    private val appInboxController: AppInboxController
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        try {
            appInboxController.getAppInboxMessages(page, pageSize, callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "getAppInboxMessages(): ", ex)
        }
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        try {
            appInboxController.getMessagesCount(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "getAppInboxMessagesCount(): ", ex)
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        try {
            appInboxController.subscribeCountChanges(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "subscribeOnMessagesCountChanged(): ", ex)
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        try {
            appInboxController.unsubscribeCountChanges(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "unsubscribeMessagesCountChanged(): ", ex)
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        try {
            appInboxController.unsubscribeAllCountChanges()
        } catch (ex: Throwable) {
            Logger.e(TAG, "unsubscribeAllMessagesCountChanged(): ", ex)
        }
    }

    override fun markAsOpened(messageId: String) {
        try {
            appInboxController.markAsOpened(messageId)
        } catch (ex: Throwable) {
            Logger.e(TAG, "markAsOpened(): ", ex)
        }
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        try {
            appInboxController.markAllMessagesAsOpened(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "markAllMessagesAsOpened(): ", ex)
        }
    }

    companion object {
        val TAG: String = AppInboxImpl::class.java.simpleName
    }
}