package com.reteno.core.data.repository

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode

interface ConfigRepository {
    fun setExternalDeviceId(externalId: String)
    fun setDeviceIdMode(mode: DeviceIdMode, onDeviceIdChanged: (DeviceId) -> Unit)
    fun getDeviceId(): DeviceId
    fun saveFcmToken(token: String)
    fun getFcmToken(): String
    fun saveDefaultNotificationChannel(channel: String)
    fun getDefaultNotificationChannel(): String
}