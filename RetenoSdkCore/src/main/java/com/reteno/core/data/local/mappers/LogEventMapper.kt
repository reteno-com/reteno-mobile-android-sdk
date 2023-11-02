package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent

internal fun RetenoLogEvent.toDb() = RetenoLogEventDb(
    platformName = platformName.toDb(),
    osVersion = osVersion,
    version = version,
    device = device,
    sdkVersion = sdkVersion,
    deviceId = deviceId,
    bundleId = bundleId,
    logLevel = logLevel.toDb(),
    errorMessage = errorMessage
)

internal fun LogLevel.toDb(): LogLevelDb =
    when (this) {
        LogLevel.DEBUG -> LogLevelDb.DEBUG
        LogLevel.INFO -> LogLevelDb.INFO
        LogLevel.WARNING -> LogLevelDb.WARNING
        LogLevel.ERROR -> LogLevelDb.ERROR
        LogLevel.FATAL -> LogLevelDb.FATAL
    }
