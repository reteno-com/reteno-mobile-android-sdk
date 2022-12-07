package com.reteno.core.data.local.database.schema

internal object AppInboxSchema {
    internal const val TABLE_NAME_APP_INBOX = "AppInbox"

    internal const val COLUMN_APP_INBOX_ID = "messageId"
    internal const val COLUMN_APP_INBOX_DEVICE_ID = "deviceId"
    internal const val COLUMN_APP_INBOX_STATUS = "status"
    internal const val COLUMN_APP_INBOX_TIME = "time"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_APP_INBOX" +
                "(" +
                "$COLUMN_APP_INBOX_ID TEXT PRIMARY KEY, " +
                "$COLUMN_APP_INBOX_DEVICE_ID TEXT, " +
                "$COLUMN_APP_INBOX_STATUS TEXT, " +
                "$COLUMN_APP_INBOX_TIME TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_APP_INBOX_ID,
        COLUMN_APP_INBOX_DEVICE_ID,
        COLUMN_APP_INBOX_STATUS,
        COLUMN_APP_INBOX_TIME
    )
}