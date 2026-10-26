package com.reteno.core.util

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log

object Logger {

    @Volatile
    private var sink: RetenoLogSink = AndroidLogSink()

    private var remoteLog: RemoteLog? = null

    private val beforeInstall = ArrayDeque<(RemoteLog) -> Unit>()

    internal fun install(sink: RetenoLogSink, remoteLog: RemoteLog?) {
        this.sink = sink
        val buffered = synchronized(beforeInstall) {
            this.remoteLog = remoteLog
            val records = beforeInstall.toList()
            beforeInstall.clear()
            records
        }
        if (remoteLog != null) {
            buffered.forEach { it(remoteLog) }
        }
    }

    @JvmStatic
    fun captureMessage(msg: String, logLevel: LogLevel = LogLevel.INFO) {
        toRemoteLog { it.record(msg, logLevel) }
    }

    @JvmStatic
    fun captureEvent(logEvent: RetenoLogEvent) {
        toRemoteLog { it.recordEvent(logEvent) }
    }

    @JvmStatic
    fun v(tag: String, methodName: String, vararg arguments: Any?) {
        sink.log(LogPriority.VERBOSE, tag) { buildMessage(methodName, arguments) }
    }

    @JvmStatic
    fun d(tag: String, methodName: String, vararg arguments: Any?) {
        sink.log(LogPriority.DEBUG, tag) { buildMessage(methodName, arguments) }
    }

    @JvmStatic
    fun i(tag: String, methodName: String, vararg arguments: Any?) {
        sink.log(LogPriority.INFO, tag) { buildMessage(methodName, arguments) }
    }

    @JvmStatic
    fun w(tag: String, methodName: String, vararg arguments: Any?) {
        sink.log(LogPriority.WARN, tag) { buildMessage(methodName, arguments) }
    }

    @JvmStatic
    fun e(tag: String, methodName: String, tr: Throwable) {
        sink.log(LogPriority.ERROR, tag) { withStackTrace(methodName, tr) }
        toRemoteLog { it.recordError(methodName, tr) }
    }

    @JvmStatic
    fun e(tag: String, methodName: String, tr: Throwable, vararg arguments: Any?) {
        sink.log(LogPriority.ERROR, tag) { withStackTrace(buildMessage(methodName, arguments), tr) }
        toRemoteLog { it.recordError(methodName, tr) }
    }

    private fun toRemoteLog(record: (RemoteLog) -> Unit) {
        val installed = synchronized(beforeInstall) {
            remoteLog ?: return keepUntilInstall(record)
        }
        record(installed)
    }

    private fun keepUntilInstall(record: (RemoteLog) -> Unit) {
        if (beforeInstall.size == MAX_BEFORE_INSTALL) {
            beforeInstall.removeFirst()
        }
        beforeInstall.addLast(record)
    }

    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String =
        arguments.joinToString(separator = "", prefix = methodName)

    private fun withStackTrace(message: String, tr: Throwable): String = "$message\n${tr.stackTraceToString()}"

    private const val MAX_BEFORE_INSTALL = 50
}
