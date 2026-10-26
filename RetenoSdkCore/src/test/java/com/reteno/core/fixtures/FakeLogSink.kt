package com.reteno.core.fixtures

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.RetenoLogSink

internal data class LogEntry(val priority: LogPriority, val tag: String, val message: String)

internal class FakeLogSink(override var isEnabled: Boolean = true) : RetenoLogSink {
    val entries = mutableListOf<LogEntry>()

    val lines: List<String>
        get() = entries.map { it.message }

    override fun write(priority: LogPriority, tag: String, message: String) {
        entries += LogEntry(priority, tag, message)
    }
}
