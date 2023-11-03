package com.reteno.core.data.repository

import com.reteno.core.domain.model.logevent.RetenoLogEvent

interface LogEventRepository {

    fun saveLogEvent(logEvent: RetenoLogEvent)

    fun pushLogEvents(limit: Int? = null)
}