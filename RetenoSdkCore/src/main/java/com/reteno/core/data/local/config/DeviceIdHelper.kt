package com.reteno.core.data.local.config

import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

class DeviceIdHelper(private val sharedPrefsManager: SharedPrefsManager) {

    internal fun withDeviceIdMode(
        currentDeviceId: DeviceId,
        deviceIdMode: DeviceIdMode,
        onDeviceIdChanged: (DeviceId) -> Unit
    ) {
        val context = RetenoImpl.application

        when (deviceIdMode) {
            DeviceIdMode.ANDROID_ID -> {
                try {
                    val deviceId = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", deviceId, "]")
                    /*@formatter:on*/
                    onDeviceIdChanged(currentDeviceId.copy(id = deviceId, mode = deviceIdMode))
                } catch (ex: java.lang.Exception) {
                    /*@formatter:off*/ Logger.e(TAG, "initDeviceId(): DeviceIdMode.ANDROID_ID", ex)
                    /*@formatter:on*/
                    withDeviceIdMode(currentDeviceId, DeviceIdMode.RANDOM_UUID, onDeviceIdChanged)
                    return
                }
            }
            DeviceIdMode.APP_SET_ID -> {
                try {
                    val client = AppSet.getClient(context)
                    client.appSetIdInfo.addOnSuccessListener {
                        /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", it.id, "]")
                        /*@formatter:on*/
                        onDeviceIdChanged(currentDeviceId.copy(id = it.id, mode = deviceIdMode))
                    }.addOnFailureListener {
                        /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " failed trying ANDROID_ID")
                        /*@formatter:on*/
                        withDeviceIdMode(currentDeviceId, DeviceIdMode.RANDOM_UUID, onDeviceIdChanged)
                    }
                } catch (ex: java.lang.Exception) {
                    /*@formatter:off*/ Logger.e(TAG, "initDeviceId(): DeviceIdMode.APP_SET_ID", ex)
                    /*@formatter:on*/
                    withDeviceIdMode(currentDeviceId, DeviceIdMode.RANDOM_UUID, onDeviceIdChanged)
                }
            }
            DeviceIdMode.RANDOM_UUID -> {
                val newId = sharedPrefsManager.getDeviceIdUuid()
                onDeviceIdChanged(currentDeviceId.copy(id = newId, mode = deviceIdMode))
                /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", newId, "]")
                /*@formatter:on*/
            }
        }
    }

    internal fun withExternalDeviceId(
        currentDeviceId: DeviceId,
        externalDeviceId: String?
    ): DeviceId {
        /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [EXTERNAL_ID]", " deviceId = [", externalDeviceId, "]")
        /*@formatter:on*/
        val externalId = externalDeviceId?.ifBlank { null }
        return currentDeviceId.copy(externalId = externalId)
    }

    companion object {
        val TAG: String = DeviceIdHelper::class.java.simpleName
    }
}