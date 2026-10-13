package com.reteno.core.data.repository

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus

internal interface AppInboxRepository {

    fun saveMessageOpened(messageId: String)
    fun setAllMessageOpened(callback: RetenoResultCallback<Unit>)
    fun getMessages(
        page: Int? = null,
        pageSize: Int? = null,
        status: AppInboxStatus? = null,
        resultCallback: RetenoResultCallback<AppInboxMessages>
    )
    fun getMessagesCount(resultCallback: RetenoResultCallback<Int>)

    fun refreshMessagesCount()

    fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>)
    fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>)
    fun unsubscribeAllMessagesCountChanged()

}