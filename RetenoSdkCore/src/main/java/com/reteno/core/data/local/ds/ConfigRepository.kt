package com.reteno.core.data.local.ds

import com.reteno.core.data.local.config.DeviceIdMode

interface ConfigRepository {
    fun setExternalDeviceId(externalId: String)
    fun changeDeviceIdMode(mode: DeviceIdMode, onIdChangedCallback: () -> Unit)
    fun getDeviceId(): String
    fun getDeviceIdMode(): DeviceIdMode
    fun getExternalId(): String?
    fun saveFcmToken(token: String)
    fun getFcmToken(): String
    fun saveDefaultNotificationChannel(channel: String)
    fun getDefaultNotificationChannel(): String
}