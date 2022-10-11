package com.reteno.push.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

fun Context.getApplicationMetaData(): Bundle =
    if (Build.VERSION.SDK_INT >= 33) {
        packageManager.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
    } else {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    }

object RetenoManifestHelper {

}