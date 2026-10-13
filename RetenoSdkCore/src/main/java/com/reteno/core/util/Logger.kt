package com.reteno.core.util

import android.util.Log
import com.reteno.core.BuildConfig
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.log.Redact

object Logger {

    @JvmStatic
    fun captureMessage(msg: String, logLevel: LogLevel = LogLevel.INFO) {
        createLogEvent(Redact.uploadable(msg), logLevel)
    }

    @JvmStatic
    fun captureEvent(logEvent: RetenoLogEvent) {
        logEvent.errorMessage = logEvent.errorMessage?.let(Redact::uploadable)
        fillEventData(logEvent)
        saveEvent(logEvent)
    }

    @JvmStatic
    fun v(tag: String, methodName: String, vararg arguments: Any?) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            val message = buildMessage(methodName, arguments)
            Log.v(tag, message)
        }
    }

    @JvmStatic
    fun d(tag: String, methodName: String, vararg arguments: Any?) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            val message = buildMessage(methodName, arguments)
            Log.d(tag, message)
        }
    }

    @JvmStatic
    fun i(tag: String, methodName: String, vararg arguments: Any?) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            val message = buildMessage(methodName, arguments)
            Log.i(tag, message)
        }
    }

    @JvmStatic
    fun w(tag: String, methodName: String, vararg arguments: Any?) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            val message = buildMessage(methodName, arguments)
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

    @JvmStatic
    fun e(tag: String, methodName: String, tr: Throwable, vararg arguments: Any?) {
        if (BuildConfig.DEBUG || Util.isDebugView()) {
            val message = buildMessage(methodName, arguments)
            Log.e(tag, message, tr)
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
        return "${Redact.uploadable(methodName)}: ${Redact.uploadableStackTrace(tr)}"
    }

    private fun createLogEvent(message: String, logLevel: LogLevel) {
        try {
            val logEvent = RetenoLogEvent()
            logEvent.errorMessage = message
            logEvent.logLevel = logLevel
            fillEventData(logEvent)
            saveEvent(logEvent)
        } catch (e: Exception) {
            Log.e(TAG, "createLogEvent: ", e)
        }
    }

    private fun fillEventData(event: RetenoLogEvent) {
        try {
            val packageName = RetenoInternalImpl.instance.application.packageName
            event.bundleId = packageName

            val deviceId = runCatching {
                RetenoInternalImpl.instance.getDeviceId()
            }.getOrElse { "uninitialized" }

            event.deviceId = deviceId
        } catch (ex: Throwable) {
            Log.e(TAG, "setApplicationTags: ", ex)
        }
    }

    private fun saveEvent(logEvent: RetenoLogEvent) {
        try {
            RetenoInternalImpl.instance.logRetenoEvent(logEvent)
        } catch (e: Exception) {
            Log.e(TAG, "saveEvent: ", e)
        }
    }
}