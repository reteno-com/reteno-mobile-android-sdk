package com.reteno.core.internal.log

import android.util.Log

internal enum class LogPriority(val androidPriority: Int) {
    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR)
}
