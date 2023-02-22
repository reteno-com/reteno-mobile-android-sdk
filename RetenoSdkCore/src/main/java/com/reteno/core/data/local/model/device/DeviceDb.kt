package com.reteno.core.data.local.model.device

import com.reteno.core.data.local.model.BooleanDb

data class DeviceDb(
    val rowId: String? = null,
    val deviceId: String,
    val externalUserId: String?,
    val pushToken: String?,
    val pushSubscribed: BooleanDb?,
    val category: DeviceCategoryDb,
    val osType: DeviceOsDb = DeviceOsDb.ANDROID,
    val osVersion: String?,
    val deviceModel: String?,
    val appVersion: String?,
    val languageCode: String?,
    val timeZone: String?,
    val advertisingId: String?
)