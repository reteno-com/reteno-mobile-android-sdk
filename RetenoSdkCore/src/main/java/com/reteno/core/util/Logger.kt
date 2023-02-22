package com.reteno.core.util

import android.util.Log
import com.google.firebase.FirebaseApp
import com.reteno.core.BuildConfig
import com.reteno.core.BuildConfig.SDK_VERSION
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import io.sentry.Hub
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions

object Logger {
    private const val SENTRY_DSN = BuildConfig.SENTRY_DSN

    private const val TAG_KEY_PACKAGE_NAME = "reteno.package_name"
    private const val TAG_KEY_SDK_VERSION = "reteno.sdk_version"
    private const val TAG_KEY_DEVICE_ID = "reteno.device_id"
    private const val TAG_KEY_DEVICE_REGISTERED = "reteno.device_registered"
    private const val TAG_KEY_PUSH_SUBSCRIBED = "reteno.push_subscribed"
    private const val TAG_KEY_GOOGLE_PLAY_AVAILABLE = "reteno.google_play_available"
    private const val TAG_KEY_FCM_AVAILABLE = "reteno.fcm_available"

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
            dsn = SENTRY_DSN
            setTag(TAG_KEY_SDK_VERSION, SDK_VERSION)
            setApplicationTags(this)
            setServicesTags(this)
        })
        return retenoHub
    }

    private fun setApplicationTags(options: SentryOptions) {
        try {
            val packageName = RetenoImpl.application.packageName
            options.setTag(TAG_KEY_PACKAGE_NAME, packageName)

            val serviceLocator =
                ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl).serviceLocator
            val configRepository = serviceLocator.configRepositoryProvider.get()

            options.setTag(TAG_KEY_DEVICE_ID, configRepository.getDeviceId().id)
            options.setTag(TAG_KEY_DEVICE_REGISTERED, configRepository.isDeviceRegistered().toString())
            options.setTag(TAG_KEY_PUSH_SUBSCRIBED, configRepository.isNotificationsEnabled().toString())
        } catch (ex: Throwable) {
            Log.e(TAG, "setApplicationTags: ", ex)
        }
    }

    private fun setServicesTags(options: SentryOptions) {
        try {
            options.setTag(TAG_KEY_FCM_AVAILABLE, isFirebaseEnabled().toString())
            options.setTag(TAG_KEY_GOOGLE_PLAY_AVAILABLE, isGooglePlayServicesAvailable().toString())
        } catch (ex: Throwable) {
            Log.e(TAG, "setServicesTags: ", ex)
        }
    }

    private fun isFirebaseEnabled(): Boolean =
        try {
            // Note: always true. Will throw exception if Firebase is not available
            FirebaseApp.getInstance() != null
        } catch (e: IllegalStateException) {
            false
        }
}