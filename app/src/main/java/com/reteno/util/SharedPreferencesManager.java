package com.reteno.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.reteno.config.DeviceIdMode;

public class SharedPreferencesManager {
    public static final String PREF_FILE_NAME = "sharedPrefs";

    public static final String PREF_KEY_DEVICE_ID_MODE = "DEVICE_ID_MODE";
    public static final String PREF_KEY_EXTERNAL_ID = "EXTERNAL_ID";

    public static void saveDeviceIdMode(Context context, DeviceIdMode deviceIdMode) {
        getPrefs(context).edit().putString(PREF_KEY_DEVICE_ID_MODE, deviceIdMode.toString()).apply();
    }

    public static DeviceIdMode getDeviceIdMode(Context context) {
        String deviceIdModeString = getPrefs(context).getString(PREF_KEY_DEVICE_ID_MODE, DeviceIdMode.APP_SET_ID.toString());
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
}
