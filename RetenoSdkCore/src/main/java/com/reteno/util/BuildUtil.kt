package com.reteno.util

import android.content.Context
import android.os.Build

internal object BuildUtil {
    private var targetSdk = -1

    /**
     * Checks whether notification trampolines are not supported.
     * Targeting Android 12 means you cannot use a service or broadcast receiver as a trampoline to
     * start an activity. The activity must be started immediately when notification is clicked.
     *
     * @param context The application context.
     * @return True if notification trampolines are not supported.
     */
    fun shouldDisableTrampolines(context: Context?): Boolean {
        return Build.VERSION.SDK_INT >= 31 && getTargetSdkVersion(context) >= 31
    }

    /**
     * Returns target SDK version parsed from manifest.
     *
     * @param context The application context.
     * @return Target SDK version.
     */
    private fun getTargetSdkVersion(context: Context?): Int {
        if (targetSdk == -1 && context != null) {
            targetSdk = context.applicationInfo.targetSdkVersion
        }
        return targetSdk
    }
}