package com.reteno.core.util

import android.app.PendingIntent
import android.content.Context
import android.os.Build

object BuildUtil {

    /**
     * Checks whether notification trampolines are not supported.
     * Targeting Android 12 means you cannot use a service or broadcast receiver as a trampoline to
     * start an activity. The activity must be started immediately when notification is clicked.
     *
     * @return True if notification trampolines are not supported.
     */
    fun shouldDisableTrampolines(context: Context): Boolean =
        trampolinesDisabled(Build.VERSION.SDK_INT, getTargetSdkVersion(context))

    internal fun trampolinesDisabled(sdkInt: Int, targetSdk: Int): Boolean = sdkInt >= 31 && targetSdk >= 31

    /**
     * Returns target SDK version parsed from manifest.
     *
     * @return Target SDK version.
     */
    fun getTargetSdkVersion(context: Context): Int = context.applicationInfo.targetSdkVersion

    /**
     * Adds immutable property to the intent flags. Mandatory when targeting API 31.
     *
     * @param flags The default flags.
     * @return Flags with additional immutable property set.
     */
    fun createIntentFlags(flags: Int): Int = flags or PendingIntent.FLAG_IMMUTABLE
}