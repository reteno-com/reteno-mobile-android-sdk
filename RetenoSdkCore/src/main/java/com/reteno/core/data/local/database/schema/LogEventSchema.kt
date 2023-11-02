package com.reteno.core.data.local.database.schema

internal object LogEventSchema {
    internal const val TABLE_NAME_LOG_EVENT = "LogEvent"

    internal const val COLUMN_LOG_EVENT_ROW_ID = "row_id"
    internal const val COLUMN_OS_TYPE = "osType"
    internal const val COLUMN_OS_VERSION = "osVersion"
    internal const val COLUMN_VERSION = "version"
    internal const val COLUMN_DEVICE = "device"
    internal const val COLUMN_SDK_VERSION = "sdkVersion"
    internal const val COLUMN_DEVICE_ID = "deviceId"
    internal const val COLUMN_BUNDLE_ID = "bundleId"
    internal const val COLUMN_LOG_LEVEL = "logLevel"
    internal const val COLUMN_ERROR_MESSAGE = "errorMessage"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_LOG_EVENT" +
                "(" +
                "$COLUMN_LOG_EVENT_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_OS_TYPE TEXT NOT NULL, " +
                "$COLUMN_OS_VERSION TEXT NOT NULL, " +
                "$COLUMN_VERSION TEXT, " +
                "$COLUMN_DEVICE TEXT NOT NULL, " +
                "$COLUMN_SDK_VERSION TEXT NOT NULL, " +
                "$COLUMN_DEVICE_ID TEXT, " +
                "$COLUMN_BUNDLE_ID TEXT, " +
                "$COLUMN_LOG_LEVEL TEXT NOT NULL, " +
                "$COLUMN_ERROR_MESSAGE TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_LOG_EVENT_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_OS_TYPE,
        COLUMN_OS_VERSION,
        COLUMN_VERSION,
        COLUMN_DEVICE,
        COLUMN_SDK_VERSION,
        COLUMN_DEVICE_ID,
        COLUMN_BUNDLE_ID,
        COLUMN_LOG_LEVEL,
        COLUMN_ERROR_MESSAGE
    )
}