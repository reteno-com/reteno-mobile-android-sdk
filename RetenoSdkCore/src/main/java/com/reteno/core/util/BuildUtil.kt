package com.reteno.core.util

import android.app.PendingIntent
import android.content.Context
import android.os.Build

object BuildUtil {
    private var targetSdk = -1

    /**
     * Checks whether notification trampolines are not supported.
     * Targeting Android 12 means you cannot use a service or broadcast receiver as a trampoline to
     * start an activity. The activity must be started immediately when notification is clicked.
     *
     * @return True if notification trampolines are not supported.
     */
    fun shouldDisableTrampolines(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= 31 && getTargetSdkVersion(context) >= 31
    }

    /**
     * Returns target SDK version parsed from manifest.
     *
     * @return Target SDK version.
     */
    fun getTargetSdkVersion(context: Context): Int {
        if (targetSdk == -1) {
            targetSdk = context.applicationInfo.targetSdkVersion
        }
        return targetSdk
    }

    /**
     * Adds immutable property to the intent flags. Mandatory when targeting API 31.
     *
     * @param flags The default flags.
     * @return Flags with additional immutable property set.
     */
    fun createIntentFlags(flags: Int): Int = flags or PendingIntent.FLAG_IMMUTABLE
}