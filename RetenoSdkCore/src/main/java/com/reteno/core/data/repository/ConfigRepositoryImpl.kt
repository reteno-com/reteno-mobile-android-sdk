package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

internal class ConfigRepositoryImpl(
    private val sharedPrefsManager: SharedPrefsManager,
    private val restConfig: RestConfig
) : ConfigRepository {

    override fun setExternalDeviceId(externalId: String) {
        restConfig.setExternalDeviceId(externalId)
    }

    override fun setDeviceIdMode(mode: DeviceIdMode, onDeviceIdChanged: (DeviceId) -> Unit) {
        restConfig.setDeviceIdMode(mode, onDeviceIdChanged)
    }

    override fun getDeviceId(): DeviceId = restConfig.deviceId

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