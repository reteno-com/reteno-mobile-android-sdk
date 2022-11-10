package com.reteno.core.data.local.config

import com.reteno.core.util.Logger

class RestConfig(private val deviceIdHelper: DeviceIdHelper, internal val accessKey: String) {

    internal var deviceId: DeviceId = DeviceId("")
        private set

    init {
        setDeviceIdMode(DeviceIdMode.ANDROID_ID) {}
    }

    internal fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: (DeviceId) -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        deviceIdHelper.withDeviceIdMode(deviceId, deviceIdMode) {
            deviceId = it
            onDeviceIdChanged.invoke(deviceId)
        }
    }

    internal fun setExternalUserId(externalUserId: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        deviceId = deviceIdHelper.withExternalUserId(deviceId, externalUserId)
    }

    companion object {
        val TAG: String = RestConfig::class.java.simpleName
    }
}