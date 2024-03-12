package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.iam.InAppMessageDb

interface RetenoDatabaseManagerInAppMessages {
    fun insertInAppMessages(inApps: List<InAppMessageDb>)
    fun getInAppMessages(limit: Int? = null): List<InAppMessageDb>
    fun getInAppMessagesCount(): Long
    fun deleteInAppMessage(inApp: InAppMessageDb): Boolean
    fun deleteAllInAppMessages()
}