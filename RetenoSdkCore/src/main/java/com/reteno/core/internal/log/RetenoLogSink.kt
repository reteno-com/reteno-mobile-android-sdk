package com.reteno.core.internal.log

import android.util.Log
import com.reteno.core.BuildConfig
import com.reteno.core.util.Util

internal interface RetenoLogSink {
    val isEnabled: Boolean

    fun write(priority: LogPriority, tag: String, message: String)
}

internal fun RetenoLogSink.log(priority: LogPriority, tag: String, message: () -> String) {
    if (isEnabled) {
        write(priority, tag, message())
    }
}

internal fun isLoggingEnabled(
    debugBuild: Boolean = BuildConfig.DEBUG,
    debugView: Boolean = Util.isDebugView()
): Boolean = debugBuild || debugView

internal class AndroidLogSink(
    private val enabled: () -> Boolean = { isLoggingEnabled() },
    private val printer: (Int, String, String) -> Unit = { priority, tag, message ->
        Log.println(priority, tag, message)
    }
) : RetenoLogSink {

    override val isEnabled: Boolean
        get() = enabled()

    override fun write(priority: LogPriority, tag: String, message: String) {
        message.chunked(MAX_CHUNK).forEach { printer(priority.androidPriority, tag, it) }
    }

    companion object {
        const val MAX_CHUNK = 4_000
    }
}
