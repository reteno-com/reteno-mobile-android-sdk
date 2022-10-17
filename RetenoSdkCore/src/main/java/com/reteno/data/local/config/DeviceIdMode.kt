package com.reteno.data.local.config

enum class DeviceIdMode {

    /**
     * Uses the ANDROID_ID from Settings.Secure
     *
     * Survives app re-install
     * Is rotated on factory reset
     * https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID
     */
    ANDROID_ID,

    /**
     * Uses the APP_SET_ID from GMS
     * (doesn't require the device to have Google Play Services installed)
     *
     * Is rotated on app re-install
     * Is rotated on factory reset
     * https://developers.google.com/android/reference/com/google/android/gms/appset/AppSetIdInfo#public-string-getid
     */
    APP_SET_ID,

    /**
     * Uses the RANDOM_UUID
     *
     * Generates a random UUID and saves it to local storage.
     * Is rotated on app storage clean
     */
    RANDOM_UUID
}