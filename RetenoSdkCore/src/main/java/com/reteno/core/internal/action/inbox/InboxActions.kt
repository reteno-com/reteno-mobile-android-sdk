package com.reteno.core.internal.action.inbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus

internal interface InboxActions {
    fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    )

    fun submitInboxCountRequest(callback: RetenoResultCallback<Int>)

    fun subscribeInboxCount(callback: RetenoResultCallback<Int>)

    fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>)

    fun unsubscribeAllInboxCount()

    fun submitInboxMessageOpened(messageId: String)

    fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>)
}
