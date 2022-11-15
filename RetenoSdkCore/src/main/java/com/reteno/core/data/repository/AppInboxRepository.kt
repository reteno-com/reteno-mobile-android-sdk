package com.reteno.core.data.repository

import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback
import java.time.ZonedDateTime

interface AppInboxRepository {

    fun saveMessageOpened(messageId: String)
    fun saveAllMessageOpened()
    fun getMessages(
        page: Int? = null,
        pageSize: Int? = null,
        resultCallback: AppInboxMessagesCallback
    )
    fun getMessagesCount(resultCallback: AppInboxMessagesCountCallback)
    fun pushMessagesStatus()
    fun clearOldMessages(outdatedTime: ZonedDateTime)

}