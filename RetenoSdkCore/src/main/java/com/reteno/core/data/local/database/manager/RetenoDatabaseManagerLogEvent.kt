package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.logevent.RetenoLogEventDb

interface RetenoDatabaseManagerLogEvent {
    fun insertLogEvent(logEvent: RetenoLogEventDb)
    fun getLogEvents(limit: Int? = null): List<RetenoLogEventDb>
    fun getLogEventsCount(): Long
    fun deleteLogEvents(logEvents: List<RetenoLogEventDb>): Boolean
}