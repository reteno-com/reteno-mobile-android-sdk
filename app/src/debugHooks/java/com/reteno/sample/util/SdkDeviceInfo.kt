package com.reteno.sample.util

import android.content.Context
import com.reteno.core._interop.DeviceIdInternal

object SdkDeviceInfo {
    const val isAvailable = true

    fun deviceId(context: Context): String = DeviceIdInternal.deviceId(context)

    fun externalUserId(context: Context): String? = DeviceIdInternal.externalUserId(context)

    fun fcmToken(context: Context): String = DeviceIdInternal.fcmToken(context)
}
