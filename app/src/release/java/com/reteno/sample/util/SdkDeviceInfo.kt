package com.reteno.sample.util

import android.content.Context

object SdkDeviceInfo {
    const val isAvailable = false

    fun deviceId(context: Context): String = ""

    fun externalUserId(context: Context): String? = null

    fun fcmToken(context: Context): String = ""
}
