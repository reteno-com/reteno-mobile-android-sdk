package com.reteno.core.appinbox

import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback
import com.reteno.core.domain.controller.AppInboxController

class AppInboxImpl(
    private val appInboxController: AppInboxController
) : AppInbox {

    override fun getAppInboxMessages(page: Int?, pageSize: Int?, callback: AppInboxMessagesCallback) {
        appInboxController.getAppInboxMessages(page, pageSize, callback)
    }

    override fun getAppInboxMessagesCount(callback: AppInboxMessagesCountCallback) {
        appInboxController.getMessagesCount(callback)
    }

    override fun observeAppInboxMessagesCount(callback: AppInboxMessagesCountCallback) {
       // TODO("Not yet implemented")
    }

    override fun markAsOpened(messageId: String) {
        appInboxController.markAsOpened(messageId)
    }

    override fun markAllMessagesAsOpened() {
        appInboxController.markAllMessagesAsOpened()
    }
}