package com.reteno.core.data.local.config

import com.reteno.core.util.Logger

class RestConfig(val deviceIdHelper: DeviceIdHelper) {

    var deviceId: DeviceId = DeviceId("")
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

    internal fun setExternalDeviceId(externalDeviceId: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalDeviceId(): ", "externalDeviceId = [" , externalDeviceId , "]")
        /*@formatter:on*/
        deviceId = deviceIdHelper.withExternalDeviceId(deviceId, externalDeviceId)
    }

    companion object {
        val TAG: String = RestConfig::class.java.simpleName
    }
}