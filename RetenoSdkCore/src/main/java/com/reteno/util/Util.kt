package com.reteno.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


internal fun isGooglePlayServicesAvailable(context: Context): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
    return resultCode == ConnectionResult.SUCCESS
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

fun Context.getApplicationMetaData(): Bundle =
    if (Build.VERSION.SDK_INT >= 33) {
        packageManager.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
    } else {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    }

fun Context.getResolveInfoList(intent: Intent) =
    if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )
    } else {
        packageManager.queryIntentActivities(intent, 0)
    }

fun Bundle?.toStringVerbose(): String {
    if (this == null) {
        return "Bundle = [null]"
    }

    val stringBuilder = java.lang.StringBuilder()
    stringBuilder.append("Bundle: [")
    for (key in keySet()) {
        stringBuilder.append(key).append(" = ").append(get(key)).append("; ")
    }
    stringBuilder.delete(stringBuilder.length - 2, stringBuilder.length)
    stringBuilder.append("]")
    return stringBuilder.toString()
}

object Util {

    fun readFromRaw(context: Context, rawResourceId: Int): String? {
        val inputStream: InputStream = context.resources.openRawResource(rawResourceId)
        val outputStream = ByteArrayOutputStream()

        val buf = ByteArray(1024)
        var len: Int
        return try {
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }
            outputStream.toString()
        } catch (e: IOException) {
            /*@formatter:off*/ Logger.e(TAG, "readTextFile(): ", e)
            /*@formatter:on*/
            null
        } finally {
            outputStream.close()
            inputStream.close()
        }
    }
}

const val TAG = "Util"