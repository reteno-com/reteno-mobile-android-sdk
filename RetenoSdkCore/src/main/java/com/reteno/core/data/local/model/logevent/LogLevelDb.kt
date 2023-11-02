package com.reteno.core.data.local.model.logevent

import com.reteno.core.domain.model.logevent.LogLevel

enum class LogLevelDb {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL;

    companion object {
        fun fromString(value: String?): LogLevelDb =
            when (value) {
                LogLevel.DEBUG.toString() -> DEBUG
                LogLevel.INFO.toString() -> INFO
                LogLevel.WARNING.toString() -> WARNING
                LogLevel.ERROR.toString() -> ERROR
                LogLevel.FATAL.toString() -> FATAL
                else -> DEBUG
            }
    }
}