package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId

interface ConfigRepository {
    fun setExternalUserId(externalId: String?)
    fun getDeviceId(): DeviceId
    fun saveFcmToken(token: String)
    fun getFcmToken(callback: (String) -> Unit)
    fun saveDefaultNotificationChannel(channel: String)
    fun getDefaultNotificationChannel(): String

    fun saveNotificationsEnabled(enabled: Boolean)
    fun isNotificationsEnabled(): Boolean

    fun saveDeviceRegistered(registered: Boolean)
    fun isDeviceRegistered(): Boolean
}