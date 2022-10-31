package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS

internal object DbUtil {

    fun ContentValues.putDevice(device: Device) {
        put(DbConstants.Device.COLUMN_DEVICE_ID, device.deviceId)
        put(DbConstants.Device.COLUMN_EXTERNAL_USER_ID, device.externalUserId)
        put(DbConstants.Device.COLUMN_PUSH_TOKEN, device.pushToken)
        put(DbConstants.Device.COLUMN_CATEGORY, device.category.toString())
        put(DbConstants.Device.COLUMN_OS_TYPE, device.osType.toString())
        put(DbConstants.Device.COLUMN_OS_VERSION, device.osVersion)
        put(DbConstants.Device.COLUMN_DEVICE_MODEL, device.deviceModel)
        put(DbConstants.Device.COLUMN_APP_VERSION, device.appVersion)
        put(DbConstants.Device.COLUMN_LANGUAGE_CODE, device.languageCode)
        put(DbConstants.Device.COLUMN_TIMEZONE, device.timeZone)
        put(DbConstants.Device.COLUMN_ADVERTISING_ID, device.advertisingId)
    }

    fun Cursor.getDevice(): Device {
        val deviceId = getString(getColumnIndex(DbConstants.Device.COLUMN_DEVICE_ID))
        val externalUserId =
            getString(getColumnIndex(DbConstants.Device.COLUMN_EXTERNAL_USER_ID))
        val pushToken = getString(getColumnIndex(DbConstants.Device.COLUMN_PUSH_TOKEN))
        val category =
            DeviceCategory.fromString(getString(getColumnIndex(DbConstants.Device.COLUMN_CATEGORY)))
        val osType =
            DeviceOS.fromString(getString(getColumnIndex(DbConstants.Device.COLUMN_OS_TYPE)))
        val osVersion = getString(getColumnIndex(DbConstants.Device.COLUMN_OS_VERSION))
        val deviceModel = getString(getColumnIndex(DbConstants.Device.COLUMN_DEVICE_MODEL))
        val appVersion = getString(getColumnIndex(DbConstants.Device.COLUMN_APP_VERSION))
        val languageCode = getString(getColumnIndex(DbConstants.Device.COLUMN_LANGUAGE_CODE))
        val timeZone = getString(getColumnIndex(DbConstants.Device.COLUMN_TIMEZONE))
        val advertisingId = getString(getColumnIndex(DbConstants.Device.COLUMN_ADVERTISING_ID))

        val device = Device(
            deviceId,
            externalUserId,
            pushToken,
            category,
            osType,
            osVersion,
            deviceModel,
            appVersion,
            languageCode,
            timeZone,
            advertisingId
        )

        return device
    }
}