package com.reteno.core.data.local.database.schema

internal object DeviceSchema {
    internal const val TABLE_NAME_DEVICE = "Device"

    internal const val COLUMN_DEVICE_ROW_ID = "row_id"
    internal const val COLUMN_DEVICE_ID = "deviceId"
    internal const val COLUMN_EXTERNAL_USER_ID = "externalUserId"
    internal const val COLUMN_PUSH_TOKEN = "pushToken"
    internal const val COLUMN_PUSH_SUBSCRIBED = "pushSubscribed"
    internal const val COLUMN_CATEGORY = "category"
    internal const val COLUMN_OS_TYPE = "osType"
    internal const val COLUMN_OS_VERSION = "osVersion"
    internal const val COLUMN_DEVICE_MODEL = "deviceModel"
    internal const val COLUMN_APP_VERSION = "appVersion"
    internal const val COLUMN_LANGUAGE_CODE = "languageCode"
    internal const val COLUMN_TIMEZONE = "timeZone"
    internal const val COLUMN_ADVERTISING_ID = "advertisingId"
    internal const val COLUMN_SYNCHRONIZED_WITH_BACKEND = "synchronizedWithBackend"
    internal const val COLUMN_EMAIL = "email"
    internal const val COLUMN_PHONE = "phone"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_DEVICE" +
                "(" +
                "$COLUMN_DEVICE_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_DEVICE_ID TEXT NOT NULL, " +
                "$COLUMN_EXTERNAL_USER_ID TEXT, " +
                "$COLUMN_PUSH_TOKEN TEXT, " +
                "$COLUMN_PUSH_SUBSCRIBED TEXT, " +
                "$COLUMN_CATEGORY TEXT NOT NULL, " +
                "$COLUMN_OS_TYPE TEXT NOT NULL, " +
                "$COLUMN_OS_VERSION TEXT, " +
                "$COLUMN_DEVICE_MODEL TEXT, " +
                "$COLUMN_APP_VERSION TEXT, " +
                "$COLUMN_LANGUAGE_CODE TEXT, " +
                "$COLUMN_TIMEZONE TEXT, " +
                "$COLUMN_ADVERTISING_ID TEXT, " +
                "$COLUMN_SYNCHRONIZED_WITH_BACKEND TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_PHONE TEXT" +
                ")"

    internal const val SQL_UPGRADE_TABLE_VERSION_2 =
        "ALTER TABLE $TABLE_NAME_DEVICE ADD COLUMN $COLUMN_PUSH_SUBSCRIBED TEXT"

    internal const val SQL_UPGRADE_TABLE_VERSION_6 =
        "ALTER TABLE $TABLE_NAME_DEVICE ADD COLUMN $COLUMN_SYNCHRONIZED_WITH_BACKEND TEXT"

    internal const val SQL_UPGRADE_TABLE_VERSION_8_EMAIL =
        "ALTER TABLE $TABLE_NAME_DEVICE ADD COLUMN $COLUMN_EMAIL TEXT"
    internal const val SQL_UPGRADE_TABLE_VERSION_8_PHONE = "ALTER TABLE $TABLE_NAME_DEVICE ADD COLUMN $COLUMN_PHONE TEXT"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_DEVICE_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_DEVICE_ID,
        COLUMN_EXTERNAL_USER_ID,
        COLUMN_PUSH_TOKEN,
        COLUMN_PUSH_SUBSCRIBED,
        COLUMN_CATEGORY,
        COLUMN_OS_TYPE,
        COLUMN_OS_VERSION,
        COLUMN_DEVICE_MODEL,
        COLUMN_APP_VERSION,
        COLUMN_LANGUAGE_CODE,
        COLUMN_TIMEZONE,
        COLUMN_ADVERTISING_ID,
        COLUMN_SYNCHRONIZED_WITH_BACKEND,
        COLUMN_PHONE,
        COLUMN_EMAIL
    )
}