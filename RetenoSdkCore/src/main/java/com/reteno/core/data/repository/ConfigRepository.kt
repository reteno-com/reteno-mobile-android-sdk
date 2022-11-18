package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId

interface ConfigRepository {
    fun setExternalUserId(externalId: String)
    fun getDeviceId(): DeviceId
    fun saveFcmToken(token: String)
    fun getFcmToken(): String
    fun saveDefaultNotificationChannel(channel: String)
    fun getDefaultNotificationChannel(): String
}