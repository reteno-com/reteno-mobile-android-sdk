package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS

fun Device.toDb() = DeviceDb(
    deviceId = deviceId,
    externalUserId = externalUserId,
    pushToken = pushToken,
    pushSubscribed = pushSubscribed?.toDb(),
    category = category.toDb(),
    osType = osType.toDb(),
    osVersion = osVersion,
    deviceModel = deviceModel,
    appVersion = appVersion,
    languageCode = languageCode,
    timeZone = timeZone,
    advertisingId = advertisingId
)

fun DeviceOS.toDb(): DeviceOsDb =
    when (this) {
        DeviceOS.ANDROID -> DeviceOsDb.ANDROID
        DeviceOS.IOS -> DeviceOsDb.IOS
    }

fun DeviceCategory.toDb(): DeviceCategoryDb =
    when (this) {
        DeviceCategory.MOBILE -> DeviceCategoryDb.MOBILE
        DeviceCategory.TABLET -> DeviceCategoryDb.TABLET
    }
