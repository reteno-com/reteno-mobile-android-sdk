package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb

interface RetenoDatabaseManagerAppInbox {
    fun insertAppInboxMessage(message: AppInboxMessageDb)
    fun getAppInboxMessages(limit: Int? = null): List<AppInboxMessageDb>
    fun getAppInboxMessagesCount(): Long
    fun deleteAppInboxMessages(appInboxMessages: List<AppInboxMessageDb>)
    fun deleteAllAppInboxMessages()
    fun deleteAppInboxMessagesByTime(outdatedTime: String): Int
}