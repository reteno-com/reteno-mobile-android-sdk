package com.reteno.core.internal.log

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.util.Logger

internal interface RemoteLog {
    fun record(message: String, level: LogLevel)

    fun recordError(message: String, error: Throwable)

    fun recordEvent(event: RetenoLogEvent)

    fun info(message: String) = record(message, LogLevel.INFO)

    fun error(message: String) = record(message, LogLevel.ERROR)
}

internal object LoggerRemoteLog : RemoteLog {
    override fun record(message: String, level: LogLevel) = Logger.captureMessage(message, level)

    override fun recordError(message: String, error: Throwable) = Logger.e(TAG, message, error)

    override fun recordEvent(event: RetenoLogEvent) = Logger.captureEvent(event)

    private const val TAG = "RemoteLog"
}
