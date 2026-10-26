package com.reteno.core.util

import android.os.Build

object DeviceInfo {

    internal fun fetchOsVersion(): String = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    internal fun fetchDeviceModel(): String = deviceModel(Build.MANUFACTURER, Build.MODEL)

    internal fun deviceModel(manufacturer: String, model: String): String =
        if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.uppercase()
        } else {
            "${manufacturer.uppercase()} $model"
        }
}
