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

fun Cursor.getLogEvent(): RetenoLogEventDb = RetenoLogEventDb(
    rowId = getString(getColumnIndex(LogEventSchema.COLUMN_LOG_EVENT_ROW_ID)),
    platformName = DeviceOsDb.fromString(getString(getColumnIndex(LogEventSchema.COLUMN_OS_TYPE))),
    osVersion = getString(getColumnIndex(LogEventSchema.COLUMN_OS_VERSION)),
    version = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_VERSION)),
    device = getString(getColumnIndex(LogEventSchema.COLUMN_DEVICE)),
    sdkVersion = getString(getColumnIndex(LogEventSchema.COLUMN_SDK_VERSION)),
    deviceId = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_DEVICE_ID)),
    bundleId = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_BUNDLE_ID)),
    logLevel = LogLevelDb.fromString(getString(getColumnIndex(LogEventSchema.COLUMN_LOG_LEVEL))),
    errorMessage = getStringOrNull(getColumnIndex(LogEventSchema.COLUMN_ERROR_MESSAGE))
)
