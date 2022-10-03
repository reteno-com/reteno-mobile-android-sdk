package com.reteno.utils

import android.util.Log
import io.sentry.Hub
import io.sentry.Sentry

internal object Logger {
    private const val TAG = "RetenoLogger"
    private const val SENTRY_DSN =
        "https://b50d9bee97814c769500ea0d9eb7aaf4@o4503903413665792.ingest.sentry.io/4503903414779904"

    @JvmStatic
    internal fun captureException(e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        retenoHub.captureException(e)
    }

    @JvmStatic
    internal fun v(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.v(TAG, message)
    }

    @JvmStatic
    internal fun d(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.d(TAG, message)
    }

    @JvmStatic
    internal fun i(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.i(TAG, message)
    }

    @JvmStatic
    internal fun w(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.w(TAG, message)
    }

    @JvmStatic
    internal fun e(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.e(TAG, message)
    }


    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }
}