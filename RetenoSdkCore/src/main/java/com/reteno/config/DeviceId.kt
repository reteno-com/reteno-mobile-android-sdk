package com.reteno.config

import android.content.Context
import android.provider.Settings
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
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", it.id, "]")
                    /*@formatter:on*/
                    id = it.id
                }.addOnFailureListener {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " failed trying ANDROID_ID")
                    /*@formatter:on*/
                    init(context, DeviceIdMode.ANDROID_ID)
                }
            }
            DeviceIdMode.ANDROID_ID -> {
                try {
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", deviceId, "]")
                    /*@formatter:on*/
                    id = deviceId
                } catch (ex: java.lang.Exception) {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " EXCEPTION = [", ex.message, "]")
                    /*@formatter:on*/
                    init(context, DeviceIdMode.RANDOM_UUID)
                    return
                }
            }
            DeviceIdMode.RANDOM_UUID -> {
                id = SharedPrefsManager.getDeviceIdUuid()
                /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", id, "]")
                /*@formatter:on*/
            }
        }

        mode = deviceIdMode
    }

    fun setExternalDeviceId(externalDeviceId: String) {
        /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [EXTERNAL_ID]", " deviceId = [", externalDeviceId, "]")
        /*@formatter:on*/
        externalId = externalDeviceId.ifBlank { null }
    }

    companion object {
        val TAG: String = DeviceId::class.java.simpleName
    }
}