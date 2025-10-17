package com.reteno.core.data.local.config

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.util.Logger

internal class RestConfig(
    private val sharedPrefsManager: SharedPrefsManager,
    private val deviceIdHelper: DeviceIdHelper
) {

    internal var deviceId: DeviceId = DeviceId("")
        private set

    internal suspend fun initDeviceId(deviceIdMode: DeviceIdMode) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        deviceIdHelper.withDeviceIdMode(deviceId, deviceIdMode) {
            deviceId = it.copy(
                externalId = sharedPrefsManager.getExternalUserId(),
                idSuffix = sharedPrefsManager.getDeviceIdSuffix(),
                email = sharedPrefsManager.getEmail(),
                phone = sharedPrefsManager.getPhone()
            )
        }
    }

    internal fun setDeviceIdSuffix(suffix: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setDeviceIdSuffix(): ", "suffix = [" , suffix , "]")
        /*@formatter:on*/
        sharedPrefsManager.saveDeviceIdSuffix(suffix)
        deviceId = deviceIdHelper.withDeviceIdSuffix(deviceId, suffix)
    }

    internal fun setExternalUserId(externalUserId: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        sharedPrefsManager.saveExternalUserId(externalUserId)
        deviceId = deviceIdHelper.withExternalUserId(deviceId, externalUserId)
    }

    internal fun setDeviceEmail(email: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setDeviceEmail(): ", "email = [" , email , "]")
        /*@formatter:on*/
        sharedPrefsManager.saveDeviceEmail(email)
        deviceId = deviceIdHelper.withEmail(deviceId, email)
    }

    internal fun setDevicePhone(phone: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setDevicePhone(): ", "phone = [" , phone , "]")
        /*@formatter:on*/
        sharedPrefsManager.saveDevicePhone(phone)
        deviceId = deviceIdHelper.withPhone(deviceId, phone)
    }

    companion object {
        private val TAG: String = RestConfig::class.java.simpleName
    }
}