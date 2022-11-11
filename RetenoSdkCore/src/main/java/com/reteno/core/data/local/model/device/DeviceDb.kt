package com.reteno.core.data.local.model.device

data class DeviceDb(
    val deviceId: String,
    val externalUserId: String?,
    val pushToken: String?,
    val category: DeviceCategoryDb,
    val osType: DeviceOsDb = DeviceOsDb.ANDROID,
    val osVersion: String?,
    val deviceModel: String?,
    val appVersion: String?,
    val languageCode: String?,
    val timeZone: String?,
    val advertisingId: String?
)