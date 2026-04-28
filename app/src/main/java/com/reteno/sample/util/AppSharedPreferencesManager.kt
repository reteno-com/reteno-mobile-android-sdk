package com.reteno.sample.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.domain.model.event.LifecycleTrackingOptions

object AppSharedPreferencesManager {
    private const val PREF_FILE_NAME = "sharedPrefs"
    private const val PREF_KEY_DEVICE_ID_MODE = "DEVICE_ID_MODE"
    private const val PREF_KEY_EXTERNAL_ID = "EXTERNAL_ID"
    private const val PREF_KEY_DEVICE_ID = "KEY_DEVICE_ID"
    private const val PREF_KEY_DEVICE_ID_DELAY = "KEY_DEVICE_ID_DELAY"
    private const val PREF_KEY_DELAY_NEXT_LAUNCH = "KEY_DELAY_NEXT_LAUNCH"
    private const val PREF_KEY_SESSION_DURATION = "KEY_SESSION_DURATION"
    private const val PREF_KEY_LIFECYCLE_OPTIONS = "KEY_LIFECYCLE_OPTIONS"

    fun saveDeviceIdMode(context: Context, deviceIdMode: DeviceIdMode) {
        getPrefs(context).edit().putString(PREF_KEY_DEVICE_ID_MODE, deviceIdMode.toString()).apply()
    }

    fun getDeviceIdMode(context: Context): DeviceIdMode {
        val deviceIdModeString =
            getPrefs(context).getString(PREF_KEY_DEVICE_ID_MODE, DeviceIdMode.ANDROID_ID.toString())
        return DeviceIdMode.valueOf(deviceIdModeString!!)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun saveExternalId(context: Context, externalId: String?) {
        getPrefs(context).edit().putString(PREF_KEY_EXTERNAL_ID, externalId).apply()
    }

    fun getExternalId(context: Context): String {
        return getPrefs(context).getString(PREF_KEY_EXTERNAL_ID, "").orEmpty()
    }

    fun saveDeviceId(context: Context, deviceId: String?) {
        getPrefs(context).edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply()
    }

    @JvmStatic
    fun getDeviceId(context: Context): String? {
        return getPrefs(context).getString(PREF_KEY_DEVICE_ID, "")
    }

    fun saveDeviceIdDelay(context: Context, delay: Int) {
        getPrefs(context).edit().putInt(PREF_KEY_DEVICE_ID_DELAY, delay).apply()
    }

    @JvmStatic
    fun getDeviceIdDelay(context: Context): Int {
        return getPrefs(context).getInt(PREF_KEY_DEVICE_ID_DELAY, 0)
    }

    @JvmStatic
    fun setDelayLaunch(context: Context, shouldDelay: Boolean) {
        getPrefs(context).edit().putBoolean(PREF_KEY_DELAY_NEXT_LAUNCH, shouldDelay).apply()
    }

    @JvmStatic
    fun getShouldDelayLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(PREF_KEY_DELAY_NEXT_LAUNCH, false)
    }

    @JvmStatic
    fun getSessionDuration(context: Context): Long {
        return getPrefs(context).getLong(PREF_KEY_SESSION_DURATION, 3 * 60L * 60L * 1000L)
    }

    @JvmStatic
    fun saveSessionDuration(context: Context, duration: Long) {
        getPrefs(context).edit().putLong(PREF_KEY_SESSION_DURATION, duration).apply()
    }

    @JvmStatic
    fun saveOptions(context: Context, options: LifecycleTrackingOptions) {
        getPrefs(context).edit {
            putBoolean(PREF_KEY_LIFECYCLE_OPTIONS + "_app", options.appLifecycleEnabled)
            putBoolean(PREF_KEY_LIFECYCLE_OPTIONS + "_fg", options.foregroundLifecycleEnabled)
            putBoolean(
                PREF_KEY_LIFECYCLE_OPTIONS + "_sesh_start",
                options.sessionStartEventsEnabled
            )
            putBoolean(PREF_KEY_LIFECYCLE_OPTIONS + "_sesh_end", options.sessionEndEventsEnabled)
            putBoolean(PREF_KEY_LIFECYCLE_OPTIONS + "_push", options.pushSubscriptionEnabled)
        }
    }

    @JvmStatic
    fun getOptions(context: Context): LifecycleTrackingOptions {
        return getPrefs(context).let {
            LifecycleTrackingOptions(
                appLifecycleEnabled = it.getBoolean(
                    PREF_KEY_LIFECYCLE_OPTIONS + "_app",
                    LifecycleTrackingOptions.DEFAULT.appLifecycleEnabled
                ),
                foregroundLifecycleEnabled = it.getBoolean(
                    PREF_KEY_LIFECYCLE_OPTIONS + "_fg",
                    LifecycleTrackingOptions.DEFAULT.foregroundLifecycleEnabled
                ),
                sessionStartEventsEnabled = it.getBoolean(
                    PREF_KEY_LIFECYCLE_OPTIONS + "_sesh_start",
                    LifecycleTrackingOptions.DEFAULT.sessionStartEventsEnabled
                ),
                sessionEndEventsEnabled = it.getBoolean(
                    PREF_KEY_LIFECYCLE_OPTIONS + "_sesh_end",
                    LifecycleTrackingOptions.DEFAULT.sessionEndEventsEnabled
                ),
                pushSubscriptionEnabled = it.getBoolean(
                    PREF_KEY_LIFECYCLE_OPTIONS + "_push",
                    LifecycleTrackingOptions.DEFAULT.pushSubscriptionEnabled
                )
            )
        }
    }
}
