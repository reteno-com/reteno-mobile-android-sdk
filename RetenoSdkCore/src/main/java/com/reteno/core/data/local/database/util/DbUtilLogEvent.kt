package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.LogEventSchema
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb

fun ContentValues.putLogEvent(logEvent: RetenoLogEventDb) {
    put(LogEventSchema.COLUMN_OS_TYPE, logEvent.platformName.toString())
    put(LogEventSchema.COLUMN_OS_VERSION, logEvent.osVersion)
    put(LogEventSchema.COLUMN_VERSION, logEvent.version)
    put(LogEventSchema.COLUMN_DEVICE, logEvent.device)
    put(LogEventSchema.COLUMN_SDK_VERSION, logEvent.sdkVersion)
    put(LogEventSchema.COLUMN_DEVICE_ID, logEvent.deviceId)
    put(LogEventSchema.COLUMN_BUNDLE_ID, logEvent.bundleId)
    put(LogEventSchema.COLUMN_LOG_LEVEL, logEvent.logLevel.toString())
    put(LogEventSchema.COLUMN_ERROR_MESSAGE, logEvent.errorMessage)
}

fun Cursor.getLogEvent(): RetenoLogEventDb? {
    val rowId = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_LOG_EVENT_ROW_ID))
    val osType = DeviceOsDb.fromString(getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_OS_TYPE)))
    val osVersion = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_OS_VERSION))
    val version = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_VERSION))
    val device = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_DEVICE))
    val sdkVersion = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_SDK_VERSION))
    val deviceId = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_DEVICE_ID))
    val bundleId = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_BUNDLE_ID))
    val logLevel = LogLevelDb.fromString(getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_LOG_LEVEL)))
    val errorMessage = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_ERROR_MESSAGE))

    return if (device != null && sdkVersion != null && osVersion != null) {
        RetenoLogEventDb(
            rowId = rowId,
            platformName = osType,
            osVersion = osVersion,
            version = version,
            device = device,
            sdkVersion = sdkVersion,
            deviceId = deviceId,
            bundleId = bundleId,
            logLevel = logLevel,
            errorMessage = errorMessage
        )
    } else {
        null
    }
}