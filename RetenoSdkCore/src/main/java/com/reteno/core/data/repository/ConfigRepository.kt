package com.reteno.core.data.repository

import android.content.pm.PackageInfo
import com.reteno.core.data.local.config.DeviceId
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {

    val notificationState: Flow<Boolean>

    fun setExternalUserId(externalId: String?)
    fun setUserPhone(phone: String?)
    fun setUserEmail(email: String?)
    fun getDeviceId(): DeviceId
    suspend fun awaitForDeviceId(): DeviceId
    fun saveFcmToken(token: String)
    suspend fun getFcmToken(): String
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
    fun getAppPackageName(): String
    fun getAppPackageInfo(): PackageInfo
}