package com.reteno.core.internal.log

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.util.Logger

internal interface RemoteLog {
    fun info(message: String)

    fun error(message: String)
}

internal object LoggerRemoteLog : RemoteLog {
    override fun info(message: String) = Logger.captureMessage(message, LogLevel.INFO)

    override fun error(message: String) = Logger.captureMessage(message, LogLevel.ERROR)
}
