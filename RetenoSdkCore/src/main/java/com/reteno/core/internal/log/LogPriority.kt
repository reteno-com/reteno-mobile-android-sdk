package com.reteno.core.internal.log

import android.util.Log

internal enum class LogPriority(val androidPriority: Int) {
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR)
}
