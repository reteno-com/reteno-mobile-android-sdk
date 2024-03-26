package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.iam.InAppMessageDb

interface RetenoDatabaseManagerInAppMessages {
    fun insertInAppMessages(inApps: List<InAppMessageDb>)
    fun getInAppMessages(limit: Int? = null): List<InAppMessageDb>
    fun getInAppMessagesCount(): Long
    fun deleteInAppMessages(inApps: List<InAppMessageDb>)
    fun deleteAllInAppMessages()
}