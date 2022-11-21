package com.reteno.core.domain.controller

import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.util.Logger

class AppInboxController(private val appInboxRepository: AppInboxRepository) {

    fun getAppInboxMessages(
        page: Int? = null,
        pageSize: Int? = null,
        messagesResultCallback: RetenoResultCallback<AppInboxMessages>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getAppInboxMessages(): ", "page = [" , page , "], pageSize = [" , pageSize , "], messagesResultCallback = [" , messagesResultCallback , "]") 
        /*@formatter:on*/
        appInboxRepository.getMessages(page, pageSize, messagesResultCallback)
    }

    fun getMessagesCount(countResultCallback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "getMessagesCount(): ", "countResultCallback = [" , countResultCallback , "]") 
        /*@formatter:on*/
        appInboxRepository.getMessagesCount(countResultCallback)
    }

    fun markAsOpened(id: String) {
        /*@formatter:off*/ Logger.i(TAG, "markAsOpened(): ", "id = [" , id , "]") 
        /*@formatter:on*/
        appInboxRepository.saveMessageOpened(id)
    }

    fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>) {
        /*@formatter:off*/ Logger.i(TAG, "markAllMessagesAsOpened(): ", "") 
        /*@formatter:on*/
        appInboxRepository.setAllMessageOpened(callback)
    }

    fun pushAppInboxMessagesStatus() {
        /*@formatter:off*/ Logger.i(TAG, "pushAppInboxMessagesStatus(): ", "") 
        /*@formatter:on*/
        appInboxRepository.pushMessagesStatus()
    }

    fun clearOldMessagesStatus() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldMessagesStatus(): ", "") 
        /*@formatter:on*/
        val outdatedTime = SchedulerUtils.getOutdatedTime()
        appInboxRepository.clearOldMessages(outdatedTime)
    }

    fun subscribeCountChanges(callback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "subscribeCountChanges(): ", "callback = [" , callback , "]")
        /*@formatter:on*/
        appInboxRepository.subscribeOnMessagesCountChanged(callback)
    }

    fun unsubscribeCountChanges(callback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeCountChanges(): ", "callback = [" , callback , "]")
        /*@formatter:on*/
        appInboxRepository.unsubscribeMessagesCountChanged(callback)
    }

    fun unsubscribeAllCountChanges() {
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeAllCountChanges(): ", "")
        /*@formatter:on*/
        appInboxRepository.unsubscribeAllMessagesCountChanged()
    }

    companion object {
        private val TAG: String = AppInboxController::class.java.simpleName
    }

}