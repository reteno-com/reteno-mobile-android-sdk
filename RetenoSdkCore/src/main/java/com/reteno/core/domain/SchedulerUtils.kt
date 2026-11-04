package com.reteno.core.domain

import com.reteno.core.BuildConfig
import com.reteno.core.util.Util

object SchedulerUtils {

    private const val KEEP_USER_AND_DEVICE_DATA_HOURS = 40L
    private const val KEEP_USER_AND_DEVICE_DATA_MINUTES_DEBUG = 5L

    fun getOutdatedDeviceAndUserTime(): Long = outdatedDeviceAndUserTime()

    internal fun outdatedDeviceAndUserTime(
        debugView: Boolean = Util.isDebugView(),
        debugBuild: Boolean = BuildConfig.DEBUG
    ): Long =
        if (debugView || debugBuild) {
            KEEP_USER_AND_DEVICE_DATA_MINUTES_DEBUG * 60L * 1000L
        } else {
            KEEP_USER_AND_DEVICE_DATA_HOURS * 60L * 60L * 1000L
        }
}