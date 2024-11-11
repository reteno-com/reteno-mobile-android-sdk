package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported

internal class AppInboxImpl(
    private val appInboxController: AppInboxController
) : AppInbox {

    override fun getAppInboxMessages(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "getAppInboxMessages(): ", "page = [", page, "], pageSize = [", pageSize, "], callback = [", callback, "]")
        /*@formatter:on*/
        try {
            appInboxController.getAppInboxMessages(page, pageSize, status, callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "getAppInboxMessages(): ", ex)
        }
    }

    override fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "getAppInboxMessagesCount(): ", "callback = [", callback, "]")
        /*@formatter:on*/
        try {
            appInboxController.getMessagesCount(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "getAppInboxMessagesCount(): ", ex)
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "subscribeOnMessagesCountChanged(): ", "callback = [", callback, "]")
        /*@formatter:on*/
        try {
            appInboxController.subscribeCountChanges(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "subscribeOnMessagesCountChanged(): ", ex)
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeMessagesCountChanged(): ", "callback = [", callback, "]")
        /*@formatter:on*/
        try {
            appInboxController.unsubscribeCountChanges(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "unsubscribeMessagesCountChanged(): ", ex)
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeAllMessagesCountChanged(): ", "")
        /*@formatter:on*/
        try {
            appInboxController.unsubscribeAllCountChanges()
        } catch (ex: Throwable) {
            Logger.e(TAG, "unsubscribeAllMessagesCountChanged(): ", ex)
        }
    }

    override fun markAsOpened(messageId: String) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "markAsOpened(): ", "messageId = [", messageId, "]")
        /*@formatter:on*/
        try {
            appInboxController.markAsOpened(messageId)
        } catch (ex: Throwable) {
            Logger.e(TAG, "markAsOpened(): ", ex)
        }
    }

    override fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "markAllMessagesAsOpened(): ", "callback = [", callback, "]")
        /*@formatter:on*/
        try {
            appInboxController.markAllMessagesAsOpened(callback)
        } catch (ex: Throwable) {
            Logger.e(TAG, "markAllMessagesAsOpened(): ", ex)
        }
    }

    companion object {
        private val TAG: String = AppInboxImpl::class.java.simpleName
    }
}