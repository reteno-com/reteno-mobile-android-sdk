package com.reteno.core.data.local.config

import com.reteno.core.RetenoConfig
import com.reteno.core.identification.DeviceIdProvider

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
     * https://developer.android.com/training/articles/app-set-id
     * https://developers.google.com/android/reference/com/google/android/gms/appset/AppSetIdInfo
     */
    APP_SET_ID,

    /**
     * Uses the RANDOM_UUID
     *
     * Generates a random UUID and saves it to local storage.
     * Is rotated on app storage clean
     */
    RANDOM_UUID,

    /**
     * Uses the Id provided by user
     *
     * Uses [DeviceIdProvider] from [RetenoConfig] as an Id provider
     */
    CLIENT_UUID
}