package com.reteno.util

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.Build
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal fun isGooglePlayServicesAvailable(context: Context): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
    return resultCode == ConnectionResult.SUCCESS
}


fun Context.getApplicationMetaData(): Bundle =
    if (Build.VERSION.SDK_INT >= 33) {
        packageManager.getApplicationInfo(
            packageName,
            ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
    } else {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    }

fun Application.getAppName(): String {
    val stringId = applicationInfo.labelRes
    val appName = if (stringId == 0) {
        applicationInfo.loadLabel(packageManager).toString()
    } else {
        getString(stringId)
    }
    /*@formatter:off*/ Logger.i(TAG, "getAppName(): ", "[", appName, "]")
    /*@formatter:on*/
    return appName
}

const val TAG = "Util"