package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.RetenoLogSink

internal class TraceBuffer(private val output: RetenoLogSink) : RetenoLogSink {

    private val entries = mutableListOf<Entry>()

    override val isEnabled: Boolean
        get() = output.isEnabled

    val isQuiet: Boolean
        @Synchronized get() = entries.all { it.priority == LogPriority.DEBUG }

    @Synchronized
    override fun write(priority: LogPriority, tag: String, message: String) {
        entries += Entry(priority, tag, message)
    }

    @Synchronized
    fun flush() {
        entries.forEach { output.write(it.priority, it.tag, it.message) }
        entries.clear()
    }

    private class Entry(val priority: LogPriority, val tag: String, val message: String)
}
