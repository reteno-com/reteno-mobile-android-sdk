package com.reteno.core.features.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages

interface AppInbox {

    /**
     *  Get AppInbox messages.
     *
     *  @param page - order number of requested page. If null will be returned all messages.
     *  @param pageSize - messages count per page. If null will be returned all messages.
     *  @param callback - returns result for this operations.
     *
     *  @see RetenoResultCallback
     *  @see com.reteno.core.domain.model.appinbox.AppInboxMessage
     *  @see com.reteno.core.domain.model.appinbox.AppInboxMessages
     */
    fun getAppInboxMessages(
        page: Int? = null,
        pageSize: Int? = null,
        status: AppInboxStatus? = null,
        callback: RetenoResultCallback<AppInboxMessages>
    )

    /**
     * Obtain AppInbox messages count once.
     * @param callback - returns result for this operations.
     *
     * @see RetenoResultCallback
     */
    fun getAppInboxMessagesCount(callback: RetenoResultCallback<Int>)

    /**
     * Obtain AppInbox messages count and observing the to value change.
     * @param callback - returns result for this operations.
     *
     * @see RetenoResultCallback
     */
    fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>)

    /**
     * Unsubscribe from AppInbox messages count changes.
     * @param callback that should be unsubscribed from changes
     *
     * @see RetenoResultCallback
     */
    fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>)

    /**
     * Unsubscribe all callbacks from AppInbox messages count changes.
     */
    fun unsubscribeAllMessagesCountChanged()

    /**
     *  Change inbox messages status on OPENED
     *
     *  @param messageId - [com.reteno.core.domain.model.appinbox.AppInboxMessage.id]
     */
    fun markAsOpened(messageId: String)


    /**
     *  Change all inbox messages status on OPENED
     */
    fun markAllMessagesAsOpened(callback: RetenoResultCallback<Unit>)

}