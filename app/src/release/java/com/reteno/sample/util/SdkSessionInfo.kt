package com.reteno.sample.util

import android.content.Context

object SdkSessionInfo {
    const val isAvailable = false

    fun sessionStartTimestamp(context: Context): Long = 0L

    fun setSessionDuration(durationMillis: Long) = Unit
}
