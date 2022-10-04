package com.reteno.util

import android.util.Log
import com.reteno.core.BuildConfig
import io.sentry.Hint
import io.sentry.Hub
import io.sentry.Sentry

internal object Logger {
    private const val TAG = "RetenoLogger"
    private const val SENTRY_DSN = BuildConfig.SENTRY_DSN

    @JvmStatic
    internal fun captureEvent(msg: String) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        retenoHub.captureMessage(msg)
    }

    @JvmStatic
    internal fun captureException(e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        retenoHub.captureException(e)
    }

    @JvmStatic
    internal fun captureException(message: String, e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        val hint = Hint()
        hint.set(message, null)
        retenoHub.captureException(e, hint)
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
    internal fun e(message: String) {
        Log.e(TAG, message)
        captureEvent(message)
    }

    @JvmStatic
    internal fun e(message: String, tr: Throwable) {
        Log.e(TAG, message, tr)
        captureException(message, tr)
    }


    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }
}