package com.reteno.core.util

import android.util.Log
import com.reteno.core.BuildConfig
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import io.sentry.Breadcrumb
import io.sentry.Hub
import io.sentry.Sentry

object Logger {
    private const val SENTRY_DSN = BuildConfig.SENTRY_DSN
    private const val BREADCRUMB_CATEGORY_CLASS = "className"
    private const val BREADCRUMB_CATEGORY_METHOD = "methodName"
    private const val BREADCRUMB_CATEGORY_DEVICE_ID = "deviceId"

    @JvmStatic
    fun captureEvent(msg: String) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        retenoHub.captureMessage(msg)
    }

    @JvmStatic
    fun captureException(e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        retenoHub.captureException(e)
    }

    @JvmStatic
    fun v(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.v(tag, message)
    }

    @JvmStatic
    fun d(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.d(tag, message)
    }

    @JvmStatic
    fun i(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.i(tag, message)
    }

    @JvmStatic
    fun w(tag: String, methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Log.w(tag, message)
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        Log.e(tag, message)
        captureEvent(message)
    }

    @JvmStatic
    fun e(tag: String, methodName: String, tr: Throwable) {
        Log.e(tag, methodName, tr)
        captureException(tag, methodName, tr)
    }

    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }

    private fun captureException(tag: String, methodName: String, e: Throwable) {
        val mainHub = Sentry.getCurrentHub().clone()
        val retenoHub = Hub(mainHub.options.apply {
            dsn = SENTRY_DSN
        })

        addBreadcrumbs(tag, methodName, retenoHub)
        retenoHub.captureException(e)
    }

    private fun addBreadcrumbs(tag: String, methodName: String, retenoHub: Hub) {
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