package com.reteno.core.domain.controller

import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback
import com.reteno.core.util.Logger

class AppInboxController(private val appInboxRepository: AppInboxRepository) {

    fun getAppInboxMessages(
        page: Int? = null,
        pageSize: Int? = null,
        messagesResultCallback: AppInboxMessagesCallback
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getAppInboxMessages(): ", "page = [" , page , "], pageSize = [" , pageSize , "], messagesResultCallback = [" , messagesResultCallback , "]") 
        /*@formatter:on*/
        appInboxRepository.getMessages(page, pageSize, messagesResultCallback)
    }

    fun getMessagesCount(countResultCallback: AppInboxMessagesCountCallback) {
        /*@formatter:off*/ Logger.i(TAG, "getMessagesCount(): ", "countResultCallback = [" , countResultCallback , "]") 
        /*@formatter:on*/
        appInboxRepository.getMessagesCount(countResultCallback)
    }

    fun markAsOpened(id: String) {
        /*@formatter:off*/ Logger.i(TAG, "markAsOpened(): ", "id = [" , id , "]") 
        /*@formatter:on*/
        appInboxRepository.saveMessageOpened(id)
    }

    fun markAllMessagesAsOpened() {
        /*@formatter:off*/ Logger.i(TAG, "markAllMessagesAsOpened(): ", "") 
        /*@formatter:on*/
        appInboxRepository.saveAllMessageOpened()
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

    companion object {
        private val TAG: String = AppInboxController::class.java.simpleName
    }

}