package com.reteno.core.data.local.config

import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.core.util.SharedPrefsManager

class DeviceId(private val sharedPrefsManager: SharedPrefsManager) {

    internal var id: String = sharedPrefsManager.getDeviceIdUuid()
        private set
    internal var mode: DeviceIdMode = DeviceIdMode.ANDROID_ID
        private set
    internal var externalId: String? = null
        private set

    init {
        changeDeviceIdMode()
    }

    internal fun changeDeviceIdMode(deviceIdMode: DeviceIdMode = DeviceIdMode.ANDROID_ID) {
        val context = RetenoImpl.application

        when (deviceIdMode) {
            DeviceIdMode.ANDROID_ID -> {
                try {
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", deviceId, "]")
                    /*@formatter:on*/
                    id = deviceId
                } catch (ex: java.lang.Exception) {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " EXCEPTION = [", ex.message, "]")
                    /*@formatter:on*/
                    changeDeviceIdMode(DeviceIdMode.RANDOM_UUID)
                    return
                }
            }
            DeviceIdMode.APP_SET_ID -> {
                val client = AppSet.getClient(context)
                client.appSetIdInfo.addOnSuccessListener {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", it.id, "]")
                    /*@formatter:on*/
                    id = it.id
                }.addOnFailureListener {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " failed trying ANDROID_ID")
                    /*@formatter:on*/
                    changeDeviceIdMode(DeviceIdMode.ANDROID_ID)
                }
            }
            DeviceIdMode.RANDOM_UUID -> {
                id = sharedPrefsManager.getDeviceIdUuid()
                /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", id, "]")
                /*@formatter:on*/
            }
        }

        mode = deviceIdMode
    }

    internal fun setExternalDeviceId(externalDeviceId: String) {
        /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [EXTERNAL_ID]", " deviceId = [", externalDeviceId, "]")
        /*@formatter:on*/
        externalId = externalDeviceId.ifBlank { null }
    }

    companion object {
        val TAG: String = DeviceId::class.java.simpleName
    }
}