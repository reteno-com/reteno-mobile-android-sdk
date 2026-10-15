package com.reteno.core.domain.model.device

data class Device(
    val deviceId: String,
    val deviceIdSuffix: String?,
    val externalUserId: String?,
    val pushToken: String?,
    val pushSubscribed: Boolean?,
    val category: DeviceCategory,
    val osType: DeviceOS = DeviceOS.ANDROID,
    val osVersion: String?,
    val deviceModel: String?,
    val appVersion: String?,
    val languageCode: String?,
    val timeZone: String?,
    val advertisingId: String?,
    val email: String?,
    val phone: String?
)
