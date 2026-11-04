package com.reteno.core.internal.platform

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

internal object TopActivity {

    fun packageOf(activity: Activity): String? = runCatching {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        topActivities(manager).firstOrNull()?.packageName
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun topActivities(manager: ActivityManager): List<ComponentName?> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.appTasks.map { it.taskInfo.topActivity }
        } else {
            manager.getRunningTasks(1).map { it.topActivity }
        }
}
