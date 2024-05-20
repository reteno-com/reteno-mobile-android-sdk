package com.reteno.sample.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.reteno.core.data.local.config.DeviceIdMode;

public class AppSharedPreferencesManager {
    public static final String PREF_FILE_NAME = "sharedPrefs";

    public static final String PREF_KEY_DEVICE_ID_MODE = "DEVICE_ID_MODE";
    public static final String PREF_KEY_EXTERNAL_ID = "EXTERNAL_ID";
    private static final String PREF_KEY_DEVICE_ID = "KEY_DEVICE_ID";
    private static final String PREF_KEY_DEVICE_ID_DELAY = "KEY_DEVICE_ID_DELAY";

    private static final String PREF_KEY_DELAY_NEXT_LAUNCH = "KEY_DELAY_NEXT_LAUNCH";

    public static void saveDeviceIdMode(Context context, DeviceIdMode deviceIdMode) {
        getPrefs(context).edit().putString(PREF_KEY_DEVICE_ID_MODE, deviceIdMode.toString()).apply();
    }

    public static DeviceIdMode getDeviceIdMode(Context context) {
        String deviceIdModeString = getPrefs(context).getString(PREF_KEY_DEVICE_ID_MODE, DeviceIdMode.ANDROID_ID.toString());
        return DeviceIdMode.valueOf(deviceIdModeString);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void saveExternalId(Context context, String externalId) {
        getPrefs(context).edit().putString(PREF_KEY_EXTERNAL_ID, externalId).apply();
    }

    public static String getExternalId(Context context) {
        return getPrefs(context).getString(PREF_KEY_EXTERNAL_ID, "");
    }

    public static void saveDeviceId(Context context, String deviceId) {
        getPrefs(context).edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply();
    }

    public static String getDeviceId(Context context) {
        return getPrefs(context).getString(PREF_KEY_DEVICE_ID, "");
    }

    public static void saveDeviceIdDelay(Context context, int delay) {
        getPrefs(context).edit().putInt(PREF_KEY_DEVICE_ID_DELAY, delay).apply();
    }

    public static int getDeviceIdDelay(Context context) {
        return getPrefs(context).getInt(PREF_KEY_DEVICE_ID_DELAY, 0);
    }

    public static void setDelayLaunch(Context context, boolean shouldDelay) {
        getPrefs(context).edit().putBoolean(PREF_KEY_DELAY_NEXT_LAUNCH, shouldDelay).apply();
    }

    public static boolean getShouldDelayLaunch(Context context) {
        return getPrefs(context).getBoolean(PREF_KEY_DELAY_NEXT_LAUNCH, false);
    }
}
