package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.internal.platform.FcmTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

internal class ConfigRepositoryImpl(
    private val sharedPrefsManager: SharedPrefsManager,
    private val restConfig: RestConfig,
    private val fcmTokens: FcmTokenSource
) : ConfigRepository {

    override val notificationState = MutableStateFlow(sharedPrefsManager.isNotificationsEnabled())

    override fun setExternalUserId(externalId: String?) {
        restConfig.setExternalUserId(externalId)
    }

    override fun setDeviceIdSuffix(suffix: String?) {
        restConfig.setDeviceIdSuffix(suffix)
    }

    override fun setUserEmail(email: String?) {
        restConfig.setDeviceEmail(email)
    }

    override fun setUserPhone(phone: String?) {
        restConfig.setDevicePhone(phone)
    }

    override fun getDeviceId(): DeviceId = restConfig.deviceId

    override suspend fun awaitForDeviceId(): DeviceId {
        while (true) {
            val deviceId = restConfig.deviceId
            if (deviceId.id.isNotEmpty()) return deviceId
            delay(DEVICE_ID_POLL_MILLIS)
        }
    }

    override fun saveFcmToken(token: String) {
        sharedPrefsManager.saveFcmToken(token)
    }

    override suspend fun getFcmToken(): String {
        val stored = sharedPrefsManager.getFcmToken()
        if (stored.isNotEmpty()) return stored
        return freshFcmToken().orEmpty()
    }

    override fun getStoredFcmToken(): String = sharedPrefsManager.getFcmToken()

    private suspend fun freshFcmToken(): String? {
        if (!fcmTokens.isAutoInitEnabled) {
            saveFcmToken("")
            return null
        }
        val fresh = fcmTokens.fetchToken() ?: return null
        if (sharedPrefsManager.getFcmToken() != fresh) {
            saveFcmToken(fresh)
        }
        return fresh
    }

    override fun saveDefaultNotificationChannel(channel: String) {
        sharedPrefsManager.saveDefaultNotificationChannel(channel)
    }

    override fun getDefaultNotificationChannel(): String = sharedPrefsManager.getDefaultNotificationChannel()

    override fun saveNotificationsEnabled(enabled: Boolean) {
        notificationState.value = enabled
        sharedPrefsManager.saveNotificationsEnabled(enabled)
    }

    override fun isNotificationsEnabled(): Boolean = sharedPrefsManager.isNotificationsEnabled()

    override fun saveDeviceRegistered(registered: Boolean) = sharedPrefsManager.saveDeviceRegistered(registered)

    override fun isDeviceRegistered(): Boolean = sharedPrefsManager.isDeviceRegistered()

    override fun getAppVersion(): String = sharedPrefsManager.getAppVersion()

    override fun saveAppVersion(version: String) {
        sharedPrefsManager.saveAppVersion(version)
    }

    override fun getAppBuildNumber(): Long = sharedPrefsManager.getAppBuildNumber()

    override fun saveAppBuildNumber(number: Long) {
        sharedPrefsManager.saveAppBuildNumber(number)
    }

    internal companion object {
        const val DEVICE_ID_POLL_MILLIS = 30L
    }
}
