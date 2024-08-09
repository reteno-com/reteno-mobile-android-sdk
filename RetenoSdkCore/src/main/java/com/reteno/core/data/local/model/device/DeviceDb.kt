package com.reteno.core.data.local.model.device

import com.reteno.core.data.local.model.BooleanDb

data class DeviceDb(
    val rowId: String? = null,
    val createdAt: Long = 0L,
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
    val advertisingId: String?,
    val isSynchronizedWithBackend: BooleanDb? = null,
    val email: String?,
    val phone: String?
)