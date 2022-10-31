package com.reteno.core.data.local.database

object DbConstants {
    internal const val DATABASE_NAME = "reteno.db"
    internal const val DATABASE_VERSION = 1

    internal const val COLUMN_TIMESTAMP = "timeStamp"

    object Device {
        internal const val TABLE_NAME_DEVICE = "Device"

        internal const val COLUMN_DEVICE_ID = "deviceId"
        internal const val COLUMN_EXTERNAL_USER_ID = "externalUserId"
        internal const val COLUMN_PUSH_TOKEN = "pushToken"
        internal const val COLUMN_CATEGORY = "category"
        internal const val COLUMN_OS_TYPE = "osType"
        internal const val COLUMN_OS_VERSION = "osVersion"
        internal const val COLUMN_DEVICE_MODEL = "deviceModel"
        internal const val COLUMN_APP_VERSION = "appVersion"
        internal const val COLUMN_LANGUAGE_CODE = "languageCode"
        internal const val COLUMN_TIMEZONE = "timeZone"
        internal const val COLUMN_ADVERTISING_ID = "advertisingId"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_TIMESTAMP,
            COLUMN_DEVICE_ID,
            COLUMN_EXTERNAL_USER_ID,
            COLUMN_PUSH_TOKEN,
            COLUMN_CATEGORY,
            COLUMN_OS_TYPE,
            COLUMN_OS_VERSION,
            COLUMN_DEVICE_MODEL,
            COLUMN_APP_VERSION,
            COLUMN_LANGUAGE_CODE,
            COLUMN_TIMEZONE,
            COLUMN_ADVERTISING_ID
        )
    }
}