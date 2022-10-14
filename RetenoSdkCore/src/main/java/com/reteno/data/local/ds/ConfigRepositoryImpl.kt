package com.reteno.data.local.ds

import com.reteno.data.local.config.DeviceIdMode
import com.reteno.data.local.config.RestConfig
import com.reteno.util.SharedPrefsManager

class ConfigRepositoryImpl(
    private val sharedPrefsManager: SharedPrefsManager,
    private val restConfig: RestConfig
) : ConfigRepository {

    override fun setExternalDeviceId(externalId: String) {
        restConfig.deviceId.setExternalDeviceId(externalId)
    }

    override fun changeDeviceIdMode(mode: DeviceIdMode) {
        restConfig.deviceId.changeDeviceIdMode(mode)
    }

    override fun getDeviceId(): String =
        restConfig.deviceId.id

    override fun getDeviceIdMode(): DeviceIdMode =
        restConfig.deviceId.mode

    override fun getExternalId(): String? =
        restConfig.deviceId.externalId

    override fun saveFcmToken(token: String) {
        sharedPrefsManager.saveFcmToken(token)
    }

    override fun getFcmToken(): String = sharedPrefsManager.getFcmToken()

    override fun saveDefaultNotificationChannel(channel: String) {
        sharedPrefsManager.saveDefaultNotificationChannel(channel)
    }

    override fun getDefaultNotificationChannel(): String =
        sharedPrefsManager.getDefaultNotificationChannel()
}