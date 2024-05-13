package com.reteno.core.util

import android.util.Log
import com.reteno.core.BuildConfig
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent

object Logger {

    @JvmStatic
    fun captureMessage(msg: String, logLevel: LogLevel = LogLevel.INFO) {
        createLogEvent(msg, logLevel)
    }

    @JvmStatic
    fun captureEvent(logEvent: RetenoLogEvent) {
        fillEventData(logEvent)
        saveEvent(logEvent)
    }

    @JvmStatic
    fun v(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            Log.v(tag, message)
        }
    }

    @JvmStatic
    fun d(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            Log.d(tag, message)
        }
    }

    @JvmStatic
    fun i(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            Log.i(tag, message)
        }
    }

    @JvmStatic
    fun w(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            Log.w(tag, message)
        }
    }

    @JvmStatic
    fun e(tag: String, methodName: String, tr: Throwable) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            Log.e(tag, methodName, tr)
        }

        createLogEvent(buildErrorMessage(methodName, tr), LogLevel.ERROR)
    }

    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }

    private fun buildErrorMessage(methodName: String, tr: Throwable): String {
        return "$methodName: ${tr.message}: ${Log.getStackTraceString(tr)}"
    }

    private fun createLogEvent(message: String, logLevel: LogLevel) {
        val logEvent = RetenoLogEvent()
        logEvent.errorMessage = message
        logEvent.logLevel = logLevel
        fillEventData(logEvent)
        saveEvent(logEvent)
    }

    private fun fillEventData(event: RetenoLogEvent) {
        try {
            val packageName = RetenoImpl.application.packageName
            event.bundleId = packageName

            val deviceId = runCatching {
                RetenoImpl.instance.getDeviceId()
            }.getOrElse { "uninitialized" }

            event.deviceId = deviceId
        } catch (ex: Throwable) {
            Log.e(TAG, "setApplicationTags: ", ex)
        }
    }

    private fun saveEvent(logEvent: RetenoLogEvent) {
        try {
            RetenoImpl.instance.logRetenoEvent(logEvent)
        } catch (e: Exception) {
            Log.e(TAG, "saveEvent: ", e)
        }
    }
}