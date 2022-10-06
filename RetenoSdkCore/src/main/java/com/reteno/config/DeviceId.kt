package com.reteno.config

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.google.android.gms.appset.AppSet
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager

class DeviceId {

    internal var id: String? = null
    internal var mode: DeviceIdMode = DeviceIdMode.APP_SET_ID
    internal var externalId: String? = null

    internal fun init(context: Context, deviceIdMode: DeviceIdMode = DeviceIdMode.APP_SET_ID) {
        when (deviceIdMode) {
            DeviceIdMode.APP_SET_ID -> {
                val client = AppSet.getClient(context)
                client.appSetIdInfo.addOnSuccessListener {
                    Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", it.id, "]")
                    id = it.id
                }.addOnFailureListener {
                    Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " failed trying ANDROID_ID")
                    init(context, DeviceIdMode.ANDROID_ID)
                }
            }
            DeviceIdMode.ANDROID_ID -> {
                try {
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", deviceId, "]")
                    id = deviceId
                } catch (ex: java.lang.Exception) {
                    Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " EXCEPTION = [", ex.message, "]")
                    init(context, DeviceIdMode.RANDOM_UUID)
                    return
                }
            }
            DeviceIdMode.RANDOM_UUID -> {
                id = SharedPrefsManager.getDeviceIdUuid()
                Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", id, "]")
            }
        }

        mode = deviceIdMode
    }

    fun setExternalDeviceId(externalDeviceId: String) {
        Logger.d(TAG, "initDeviceId(): ", "deviceIdMode = [EXTERNAL_ID]", " deviceId = [", externalDeviceId, "]")
        externalId = externalDeviceId
    }

    companion object {
        val TAG: String = DeviceId::class.java.simpleName
    }
}