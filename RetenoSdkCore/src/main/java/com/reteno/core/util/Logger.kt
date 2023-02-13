package com.reteno.core.util

import android.util.Log
import com.reteno.core.BuildConfig
import com.reteno.core.BuildConfig.SDK_VERSION
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import io.sentry.Breadcrumb
import io.sentry.Hub
import io.sentry.Sentry
import io.sentry.SentryEvent

object Logger {
    private const val SENTRY_DSN = BuildConfig.SENTRY_DSN
    private const val TAG_KEY_RELEASE_SDK = "release_sdk"
    private const val BREADCRUMB_CATEGORY_CLASS = "className"
    private const val BREADCRUMB_CATEGORY_METHOD = "methodName"
    private const val BREADCRUMB_CATEGORY_DEVICE_ID = "deviceId"

    @JvmStatic
    fun captureMessage(msg: String) {
        getRetenoHub().captureMessage(msg)
    }

    @JvmStatic
    fun captureEvent(event: SentryEvent) {
        getRetenoHub().captureEvent(event)
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
        val retenoHub: Hub = getRetenoHub()

        try {
            addExceptionBreadcrumbs(tag, methodName, retenoHub)
            addExceptionBreadcrumbs(tag, "Google Play Services installed = ${isGooglePlayServicesAvailable()}", retenoHub)
        } catch (ex: Throwable) {
            Log.e(TAG, "captureException: ", ex)
        }
        retenoHub.captureException(tr)
    }

    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }

    private fun getRetenoHub(): Hub {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            setTag(TAG_KEY_RELEASE_SDK, SDK_VERSION)
            dsn = SENTRY_DSN
        })
        return retenoHub
    }

    private fun addExceptionBreadcrumbs(tag: String, methodName: String, retenoHub: Hub) {
        val classBreadcrumb = Breadcrumb.debug(tag).apply {
            category = BREADCRUMB_CATEGORY_CLASS
        }

        val methodBreadcrumb = Breadcrumb.debug(methodName).apply {
            category = BREADCRUMB_CATEGORY_METHOD
        }

        val serviceLocator =
            ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl).serviceLocator
        val deviceId = serviceLocator.configRepositoryProvider.get().getDeviceId()
        val deviceIdBreadcrumb = Breadcrumb.debug(BREADCRUMB_CATEGORY_DEVICE_ID).apply {
            category = BREADCRUMB_CATEGORY_DEVICE_ID
            setData("id", deviceId.id)
            setData("mode", deviceId.mode)
            deviceId.externalId?.let { externalId ->
                setData("externalId", externalId)
            }
        }

        retenoHub.addBreadcrumb(classBreadcrumb)
        retenoHub.addBreadcrumb(methodBreadcrumb)
        retenoHub.addBreadcrumb(deviceIdBreadcrumb)
    }
}