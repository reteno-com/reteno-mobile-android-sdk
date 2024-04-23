package com.reteno.core.data.local.config

import com.reteno.core.util.Logger

internal class RestConfig(
    private val deviceIdHelper: DeviceIdHelper,
    internal val accessKey: String,
    initIdMode: DeviceIdMode
) {

    internal var deviceId: DeviceId = DeviceId("")
        private set

    init {
        initDeviceId(initIdMode)
    }

    private fun initDeviceId(deviceIdMode: DeviceIdMode) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        deviceIdHelper.withDeviceIdMode(deviceId, deviceIdMode) {
            deviceId = it
        }
    }

    internal fun setExternalUserId(externalUserId: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        deviceId = deviceIdHelper.withExternalUserId(deviceId, externalUserId)
    }

    companion object {
        private val TAG: String = RestConfig::class.java.simpleName
    }
}