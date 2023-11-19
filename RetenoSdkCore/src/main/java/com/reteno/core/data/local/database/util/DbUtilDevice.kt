package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.util.Util

fun ContentValues.putDevice(device: DeviceDb) {
    put(DeviceSchema.COLUMN_DEVICE_ID, device.deviceId)
    put(DeviceSchema.COLUMN_EXTERNAL_USER_ID, device.externalUserId)
    put(DeviceSchema.COLUMN_PUSH_TOKEN, device.pushToken)
    put(DeviceSchema.COLUMN_PUSH_SUBSCRIBED, device.pushSubscribed?.toString())
    put(DeviceSchema.COLUMN_CATEGORY, device.category.toString())
    put(DeviceSchema.COLUMN_OS_TYPE, device.osType.toString())
    put(DeviceSchema.COLUMN_OS_VERSION, device.osVersion)
    put(DeviceSchema.COLUMN_DEVICE_MODEL, device.deviceModel)
    put(DeviceSchema.COLUMN_APP_VERSION, device.appVersion)
    put(DeviceSchema.COLUMN_LANGUAGE_CODE, device.languageCode)
    put(DeviceSchema.COLUMN_TIMEZONE, device.timeZone)
    put(DeviceSchema.COLUMN_ADVERTISING_ID, device.advertisingId)
    put(DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND, device.isSynchronizedWithBackend?.toString())
}

fun Cursor.getDevice(): DeviceDb? {
    val rowId = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_DEVICE_ROW_ID))
    val createdAt = getStringOrNull(getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
    val deviceId = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_DEVICE_ID))
    val externalUserId = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_EXTERNAL_USER_ID))
    val pushToken = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_PUSH_TOKEN))
    val pushSubscribedString = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_PUSH_SUBSCRIBED))
    val pushSubscribed: BooleanDb? = BooleanDb.fromString(pushSubscribedString)
    val category = DeviceCategoryDb.fromString(getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_CATEGORY)))
    val osType = DeviceOsDb.fromString(getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_OS_TYPE)))
    val osVersion = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_OS_VERSION))
    val deviceModel = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_DEVICE_MODEL))
    val appVersion = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_APP_VERSION))
    val languageCode = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_LANGUAGE_CODE))
    val timeZone = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_TIMEZONE))
    val advertisingId = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_ADVERTISING_ID))
    val synchronizedWithBackendString = getStringOrNull(getColumnIndex(DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND))
    val synchronizedWithBackend: BooleanDb? = BooleanDb.fromString(synchronizedWithBackendString)

    return if (deviceId == null || createdAt == null) {
        null
    } else {
        DeviceDb(
            rowId = rowId,
            createdAt = Util.formatSqlDateToTimestamp(createdAt),
            deviceId = deviceId,
            externalUserId = externalUserId,
            pushToken = pushToken,
            pushSubscribed = pushSubscribed,
            category = category,
            osType = osType,
            osVersion = osVersion,
            deviceModel = deviceModel,
            appVersion = appVersion,
            languageCode = languageCode,
            timeZone = timeZone,
            advertisingId = advertisingId,
            isSynchronizedWithBackend = synchronizedWithBackend
        )
    }
}