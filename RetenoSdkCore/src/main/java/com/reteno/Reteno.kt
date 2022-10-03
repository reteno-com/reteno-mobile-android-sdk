package com.reteno

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.NonNull
import com.google.android.gms.appset.AppSet
import com.reteno.config.DeviceIdMode
import com.reteno.config.RestConfig
import com.reteno.utils.Logger
import java.util.*


object Reteno {

    @Synchronized
    @JvmOverloads
    fun init(context: Context, deviceIdMode: DeviceIdMode = DeviceIdMode.APP_SET_ID, @NonNull customDeviceId: String = "") {
        try {
            initInBackground(context, deviceIdMode, customDeviceId)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
        }
    }

    private fun initInBackground(context: Context, deviceIdMode: DeviceIdMode, customDeviceId: String = "") {
        // TODO: Move this to background thread
        initDeviceId(context, deviceIdMode, customDeviceId)
    }

    private fun initDeviceId(context: Context, deviceIdMode: DeviceIdMode, customDeviceId: String = "") {
        when(deviceIdMode) {
            DeviceIdMode.CUSTOM_ID -> {
                if (TextUtils.isEmpty(customDeviceId)) {
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "] customDeviceId is empty")
                    initDeviceId(context, DeviceIdMode.ANDROID_ID)
                } else {
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " deviceId = [", customDeviceId, "]")
                    RestConfig.deviceId = customDeviceId
                }
            }
            DeviceIdMode.APP_SET_ID -> {
                val client = AppSet.getClient(context)
                client.appSetIdInfo.addOnSuccessListener {
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " deviceId = [", it.id, "]")
                    RestConfig.deviceId = it.id
                }.addOnFailureListener {
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " failed trying ANDROID_ID")
                    initDeviceId(context, DeviceIdMode.ANDROID_ID)
                }
            }
            DeviceIdMode.ANDROID_ID -> {
                try {
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " deviceId = [", deviceId, "]")
                    RestConfig.deviceId = deviceId
                } catch (ex: java.lang.Exception) {
                    Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " EXCEPTION = [", ex.message, "]")
                    initDeviceId(context, DeviceIdMode.RANDOM_UUID)
                }
            }
            DeviceIdMode.RANDOM_UUID -> {
                val deviceId = UUID.randomUUID().toString()
                Logger.d("initDeviceId(): ", "deviceIdMode = [" , deviceIdMode , "]", " deviceId = [", deviceId, "]")
                RestConfig.deviceId = deviceId

                // TODO: Save to SharedPrefs
            }
        }





    }
}