package com.reteno.core.data.repository

import java.time.ZonedDateTime

internal interface EventsRepository {
    fun pushEvents(limit: Int? = null)
    fun clearOldEvents(outdatedTime: ZonedDateTime)

}