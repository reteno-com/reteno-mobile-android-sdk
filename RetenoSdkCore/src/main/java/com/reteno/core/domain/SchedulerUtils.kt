package com.reteno.core.domain

import com.reteno.core.util.Util
import java.time.ZonedDateTime

object SchedulerUtils {

    private const val KEEP_DATA_HOURS = 24L
    private const val KEEP_DATA_HOURS_DEBUG = 1L

    fun getOutdatedTime(): ZonedDateTime {
        val keepDataHours = if (Util.isDebugView()) KEEP_DATA_HOURS_DEBUG else KEEP_DATA_HOURS
        return ZonedDateTime.now().minusHours(keepDataHours)
    }
}