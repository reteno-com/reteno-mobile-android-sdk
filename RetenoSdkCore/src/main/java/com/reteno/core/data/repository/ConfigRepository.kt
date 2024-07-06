package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {

    val notificationState: Flow<Boolean?>

    fun setExternalUserId(externalId: String?)
    fun getDeviceId(): DeviceId
    suspend fun awaitForDeviceId(): DeviceId
    fun saveFcmToken(token: String)
    fun getFcmToken(callback: (String) -> Unit)
    fun saveDefaultNotificationChannel(channel: String)
    fun getDefaultNotificationChannel(): String

    fun saveNotificationsEnabled(enabled: Boolean)
    fun isNotificationsEnabled(): Boolean

    fun saveDeviceRegistered(registered: Boolean)
    fun isDeviceRegistered(): Boolean
    fun getAppVersion(): String
    fun saveAppVersion(version: String)
    fun getAppBuildNumber(): Long
    fun saveAppBuildNumber(number: Long)
}