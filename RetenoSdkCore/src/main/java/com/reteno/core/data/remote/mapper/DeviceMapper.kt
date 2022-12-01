package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.remote.model.device.DeviceCategoryRemote
import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.data.remote.model.device.DeviceRemote

fun DeviceDb.toRemote() = DeviceRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    pushToken = pushToken,
    pushSubscribed = pushSubscribed?.toRemote(),
    category = category.toRemote(),
    osType = osType.toRemote(),
    osVersion = osVersion,
    deviceModel = deviceModel,
    appVersion = appVersion,
    languageCode = languageCode,
    timeZone = timeZone,
    advertisingId = advertisingId
)

fun DeviceOsDb.toRemote(): DeviceOsRemote =
    when (this) {
        DeviceOsDb.ANDROID -> DeviceOsRemote.ANDROID
        DeviceOsDb.IOS -> DeviceOsRemote.IOS
    }

fun DeviceCategoryDb.toRemote(): DeviceCategoryRemote =
    when (this) {
        DeviceCategoryDb.MOBILE -> DeviceCategoryRemote.MOBILE
        DeviceCategoryDb.TABLET -> DeviceCategoryRemote.TABLET
    }
