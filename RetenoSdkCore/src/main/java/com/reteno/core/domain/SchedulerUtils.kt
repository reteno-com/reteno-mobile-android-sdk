package com.reteno.core.domain

import com.reteno.core.BuildConfig
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Util
import java.time.ZonedDateTime

object SchedulerUtils {

    private const val KEEP_DATA_HOURS = 24L
    private const val KEEP_DATA_HOURS_DEBUG = 1L

    private const val KEEP_USER_AND_DEVICE_DATA_HOURS = 40L
    private const val KEEP_USER_AND_DEVICE_DATA_MINUTES_DEBUG = 5L

    fun getOutdatedTime(): ZonedDateTime {
        val keepDataHours = if (Util.isDebugView()) KEEP_DATA_HOURS_DEBUG else KEEP_DATA_HOURS
        return ZonedDateTime.now().minusHours(keepDataHours)
    }

    fun getOutdatedDeviceAndUserTime(): Long {
        return if (Util.isDebugView() || BuildConfig.DEBUG) {
            KEEP_USER_AND_DEVICE_DATA_MINUTES_DEBUG * 60L * 1000L
        } else {
            KEEP_USER_AND_DEVICE_DATA_HOURS * 60L * 60L * 1000L
        }
    }
}