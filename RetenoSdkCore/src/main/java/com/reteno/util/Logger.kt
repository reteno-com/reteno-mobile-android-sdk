package com.reteno.util

import android.util.Log
import com.reteno.core.BuildConfig
import io.sentry.Hint
import io.sentry.Hub
import io.sentry.Sentry

internal object Logger {
    private const val SENTRY_DSN = BuildConfig.SENTRY_DSN
    private const val HINT_KEY_TAG = "tag"
    private const val HINT_TAG_MESSAGE = "message"

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
    internal fun captureException(tag: String, message: String, e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        val hint = Hint()
        hint.set(HINT_KEY_TAG, tag)
        hint.set(HINT_TAG_MESSAGE, message)
        retenoHub.captureException(e, hint)
    }

    @JvmStatic
    internal fun v(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.v(tag, message)
    }

    @JvmStatic
    internal fun d(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.d(tag, message)
    }

    @JvmStatic
    internal fun i(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.i(tag, message)
    }

    @JvmStatic
    internal fun w(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.w(tag, message)
    }

    @JvmStatic
    internal fun e(tag: String, message: String) {
        Log.e(tag, message)
        captureEvent(message)
    }

    @JvmStatic
    internal fun e(tag: String, message: String, tr: Throwable) {
        Log.e(tag, message, tr)
        captureException(tag, message, tr)
    }


    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }
}