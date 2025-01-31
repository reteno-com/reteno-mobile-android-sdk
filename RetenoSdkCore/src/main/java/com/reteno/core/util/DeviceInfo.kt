package com.reteno.core.util

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.reteno.core.Reteno
import com.reteno.core.RetenoImpl

object DeviceInfo {
    internal fun fetchOsVersion(): String {
        val osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        /*@formatter:off*/ Logger.i(TAG, "fetchOsVersion(): ", osVersion)
        /*@formatter:on*/
        return osVersion
    }


    internal fun fetchDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        val deviceModel = if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.uppercase()
        } else {
            "${manufacturer.uppercase()} $model"
        }

        /*@formatter:off*/ Logger.i(TAG, "fetchDeviceModel(): ", deviceModel)
        /*@formatter:on*/
        return deviceModel
    }

    internal fun fetchAppVersion(): String? =
        try {
            val context = RetenoImpl.instance.application

            val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = pInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(pInfo)


            val appVersion = "$versionName ($versionCode)"
            /*@formatter:off*/ Logger.i(TAG, "fetchAppVersion(): ", appVersion)
            /*@formatter:on*/
            appVersion
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.e(TAG, "fetchAppVersion(): ", e)
            null
        }

    private val TAG = DeviceInfo::class.java.simpleName
}