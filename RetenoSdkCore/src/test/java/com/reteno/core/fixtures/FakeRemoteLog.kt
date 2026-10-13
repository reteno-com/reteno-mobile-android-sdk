package com.reteno.core.fixtures

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.internal.log.RemoteLog

internal class FakeRemoteLog : RemoteLog {
    val entries = mutableListOf<Pair<String, LogLevel>>()

    val infos: List<String>
        get() = entries.filter { it.second == LogLevel.INFO }.map { it.first }

    override fun info(message: String) {
        entries += message to LogLevel.INFO
    }

    override fun error(message: String) {
        entries += message to LogLevel.ERROR
    }
}
