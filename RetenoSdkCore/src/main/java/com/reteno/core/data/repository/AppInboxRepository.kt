package com.reteno.core.data.repository

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import java.time.ZonedDateTime

internal interface AppInboxRepository {

    fun saveMessageOpened(messageId: String)
    fun setAllMessageOpened(callback: RetenoResultCallback<Unit>)
    fun getMessages(
        page: Int? = null,
        pageSize: Int? = null,
        resultCallback: RetenoResultCallback<AppInboxMessages>
    )
    fun getMessagesCount(resultCallback: RetenoResultCallback<Int>)
    fun pushMessagesStatus()
    fun clearOldMessages(outdatedTime: ZonedDateTime)

    fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>)
    fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>)
    fun unsubscribeAllMessagesCountChanged()

}