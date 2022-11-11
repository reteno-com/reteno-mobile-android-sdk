package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.remote.model.device.DeviceCategoryRemote
import com.reteno.core.data.remote.model.device.DeviceRemote
import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS

fun Device.toRemote() = DeviceRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    pushToken = pushToken,
    category = category.toRemote(),
    osType = osType.toRemote(),
    osVersion = osVersion,
    deviceModel = deviceModel,
    appVersion = appVersion,
    languageCode = languageCode,
    timeZone = timeZone,
    advertisingId = advertisingId
)

fun DeviceOS.toRemote(): DeviceOsRemote =
    when (this) {
        DeviceOS.ANDROID -> DeviceOsRemote.ANDROID
        DeviceOS.IOS -> DeviceOsRemote.IOS
    }

fun DeviceCategory.toRemote(): DeviceCategoryRemote =
    when (this) {
        DeviceCategory.MOBILE -> DeviceCategoryRemote.MOBILE
        DeviceCategory.TABLET -> DeviceCategoryRemote.TABLET
    }


//==================================================================================================
fun DeviceDb.toRemote() = DeviceRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    pushToken = pushToken,
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
