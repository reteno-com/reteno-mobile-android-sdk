package com.reteno.sample.util

import android.content.Context
import com.reteno.core._interop.SessionInfoInternal

object SdkSessionInfo {
    const val isAvailable = true

    fun sessionStartTimestamp(context: Context): Long = SessionInfoInternal.sessionStartTimestamp(context)

    fun setSessionDuration(durationMillis: Long) = SessionInfoInternal.setSessionDuration(durationMillis)
}
