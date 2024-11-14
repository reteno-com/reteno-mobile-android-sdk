package com.reteno.sample.util

import android.content.Context
import android.content.SharedPreferences
import com.reteno.core.data.local.config.DeviceIdMode

object AppSharedPreferencesManager {
    private const val PREF_FILE_NAME = "sharedPrefs"
    private const val PREF_KEY_DEVICE_ID_MODE = "DEVICE_ID_MODE"
    private const val PREF_KEY_EXTERNAL_ID = "EXTERNAL_ID"
    private const val PREF_KEY_DEVICE_ID = "KEY_DEVICE_ID"
    private const val PREF_KEY_DEVICE_ID_DELAY = "KEY_DEVICE_ID_DELAY"
    private const val PREF_KEY_DELAY_NEXT_LAUNCH = "KEY_DELAY_NEXT_LAUNCH"
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
}
