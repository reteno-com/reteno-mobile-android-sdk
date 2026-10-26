package com.reteno.core.fixtures

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.log.RemoteLog

internal class FakeRemoteLog : RemoteLog {
    val entries = mutableListOf<Pair<String, LogLevel>>()
    val errors = mutableListOf<Pair<String, Throwable>>()
    val events = mutableListOf<RetenoLogEvent>()

    val infos: List<String>
        get() = entries.filter { it.second == LogLevel.INFO }.map { it.first }

    override fun record(message: String, level: LogLevel) {
        entries += message to level
    }

    override fun recordError(message: String, error: Throwable) {
        errors += message to error
    }

    override fun recordEvent(event: RetenoLogEvent) {
        events += event
    }
}
