package com.reteno.core.data.local.database.schema

object InAppMessageSchema {
    internal const val TABLE_NAME_IN_APP_MESSAGE = "InAppMessage"

    internal const val COLUMN_IAM_ROW_ID = "row_id"
    internal const val COLUMN_IAM_ID = "iamId"
    internal const val COLUMN_IAM_INSTANCE_ID = "iamInstanceId"
    internal const val COLUMN_IAM_DISPLAY_RULES = "iamDisplayRules"
    internal const val COLUMN_IAM_LAST_SHOW_TIME = "iamLastShowTime"
    internal const val COLUMN_IAM_SHOW_COUNT = "iamShowCount"
    internal const val COLUMN_IAM_LAYOUT_TYPE = "iamLayoutType"
    internal const val COLUMN_IAM_MODEL = "iamModel"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_IN_APP_MESSAGE" +
                "(" +
                "$COLUMN_IAM_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_IAM_ID INTEGER NOT NULL, " +
                "$COLUMN_IAM_INSTANCE_ID INTEGER NOT NULL, " +
                "$COLUMN_IAM_DISPLAY_RULES TEXT NOT NULL, " +
                "$COLUMN_IAM_LAST_SHOW_TIME INTEGER, " +
                "$COLUMN_IAM_SHOW_COUNT INTEGER NOT NULL, " +
                "$COLUMN_IAM_LAYOUT_TYPE TEXT, " +
                "$COLUMN_IAM_MODEL TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_IAM_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_IAM_ID,
        COLUMN_IAM_INSTANCE_ID,
        COLUMN_IAM_DISPLAY_RULES,
        COLUMN_IAM_LAST_SHOW_TIME,
        COLUMN_IAM_SHOW_COUNT,
        COLUMN_IAM_LAYOUT_TYPE,
        COLUMN_IAM_MODEL
    )
}