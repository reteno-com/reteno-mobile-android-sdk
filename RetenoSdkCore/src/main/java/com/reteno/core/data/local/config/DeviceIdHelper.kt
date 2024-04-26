package com.reteno.core.data.local.config

import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeviceIdHelper(
    private val sharedPrefsManager: SharedPrefsManager,
    private val userIdProvider: DeviceIdProvider?
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                } catch (ex: Exception) {
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
                        withDeviceIdMode(
                            currentDeviceId,
                            DeviceIdMode.RANDOM_UUID,
                            onDeviceIdChanged
                        )
                    }
                } catch (ex: Exception) {
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

            DeviceIdMode.CLIENT_UUID -> {
                if (userIdProvider == null) {
                    /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " failed trying DeviceIdProvider is null")
                    /*@formatter:on*/
                    withDeviceIdMode(
                        currentDeviceId,
                        DeviceIdMode.RANDOM_UUID,
                        onDeviceIdChanged
                    )
                    return
                }
                scope.launch {
                    try {
                        var deviceId: String? = null
                        while (deviceId == null && isActive) {
                            deviceId = userIdProvider.getDeviceId()
                            delay(60L)
                        }
                        withContext(Dispatchers.Main) {
                            /*@formatter:off*/ Logger.i(TAG, "initDeviceId(): ", "deviceIdMode = [", deviceIdMode, "]", " deviceId = [", deviceId, "]")
                            /*@formatter:on*/
                            onDeviceIdChanged(
                                currentDeviceId.copy(
                                    id = requireNotNull(deviceId),
                                    mode = deviceIdMode
                                )
                            )
                        }
                    } catch (e: Exception) {
                        withDeviceIdMode(
                            currentDeviceId,
                            DeviceIdMode.RANDOM_UUID,
                            onDeviceIdChanged
                        )
                    }
                }
            }
        }
    }

    internal fun withExternalUserId(
        currentDeviceId: DeviceId,
        externalUserId: String?
    ): DeviceId {
        /*@formatter:off*/ Logger.i(TAG, "withExternalUserId(): ", "currentDeviceId = [" , currentDeviceId , "], externalUserId = [" , externalUserId , "]") 
        /*@formatter:on*/
        val externalId = externalUserId?.ifBlank { "" }
        return currentDeviceId.copy(externalId = externalId)
    }

    companion object {
        private val TAG: String = DeviceIdHelper::class.java.simpleName
    }
}