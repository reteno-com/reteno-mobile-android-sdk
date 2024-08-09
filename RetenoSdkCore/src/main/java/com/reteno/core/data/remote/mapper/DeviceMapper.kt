package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.remote.model.device.DeviceCategoryRemote
import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.data.remote.model.device.DeviceRemote
import com.reteno.core.domain.model.device.Device

internal fun DeviceDb.toRemote() = DeviceRemote(
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
    advertisingId = advertisingId,
    email = email,
    phone = phone
)

internal fun DeviceDb.toDevice() = Device.createDevice(
    deviceId = deviceId,
    externalUserId = externalUserId,
    pushToken = pushToken,
    pushSubscribed = pushSubscribed?.toRemote(),
    advertisingId = advertisingId,
    email = email,
    phone = phone
)

internal fun DeviceOsDb.toRemote(): DeviceOsRemote =
    when (this) {
        DeviceOsDb.ANDROID -> DeviceOsRemote.ANDROID
        DeviceOsDb.IOS -> DeviceOsRemote.IOS
    }

internal fun DeviceCategoryDb.toRemote(): DeviceCategoryRemote =
    when (this) {
        DeviceCategoryDb.MOBILE -> DeviceCategoryRemote.MOBILE
        DeviceCategoryDb.TABLET -> DeviceCategoryRemote.TABLET
    }
