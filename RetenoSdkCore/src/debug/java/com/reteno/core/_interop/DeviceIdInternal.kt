package com.reteno.core._interop

import android.content.Context
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

object DeviceIdInternal {

    fun deviceId(context: Context): String = prefs(context).getLastStoredId()

    fun externalUserId(context: Context): String? = prefs(context).getExternalUserId()

    fun fcmToken(context: Context): String = prefs(context).getFcmToken()

    private fun prefs(context: Context) = SharedPrefsManager(context.applicationContext)
}
