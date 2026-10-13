package com.reteno.core.internal.platform

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

internal class AppInfo(private val context: Context) {

    fun installedVersion(): AppVersion = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppVersion(info.versionName.orEmpty(), PackageInfoCompat.getLongVersionCode(info))
    } catch (unknown: PackageManager.NameNotFoundException) {
        AppVersion("", 0L)
    }
}
