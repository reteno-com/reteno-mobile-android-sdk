package com.reteno.core.appinbox

import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback

interface AppInbox {

    /**
     *  Get AppInbox messages.
     *
     *  @param page - order number of requested page. If null will be returned all messages.
     *  @param pageSize - messages count per page. If null will be returned all messages.
     *  @param callback - returns result for this operations.
     *
     *  @see AppInboxMessagesCallback
     *  @see com.reteno.core.domain.model.appinbox.AppInboxMessage
     *  @see com.reteno.core.domain.model.appinbox.AppInboxMessages
     */
    fun getAppInboxMessages(page: Int? = null, pageSize: Int? = null, callback: AppInboxMessagesCallback)

    /**
     * Obtain AppInbox messages count once.
     * @param callback - returns result for this operations.
     *
     * @see AppInboxMessagesCountCallback
     */
    fun getAppInboxMessagesCount(callback: AppInboxMessagesCountCallback)

    /**
     * Obtain AppInbox messages count and observing the to value change.
     * @param callback - returns result for this operations.
     *
     * @see AppInboxMessagesCountCallback
     */
    fun observeAppInboxMessagesCount(callback: AppInboxMessagesCountCallback)

    /**
     *  Change inbox messages status on OPENED
     *
     *  @param messageId - [com.reteno.core.domain.model.appinbox.AppInboxMessage.id]
     */
    fun markAsOpened(messageId: String)


    /**
     *  Change all inbox messages status on OPENED
     */
    fun markAllMessagesAsOpened()

}