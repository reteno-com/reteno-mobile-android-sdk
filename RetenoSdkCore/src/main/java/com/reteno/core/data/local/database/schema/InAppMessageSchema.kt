package com.reteno.core.data.local.database.schema

object InAppMessageSchema {
    internal const val TABLE_NAME = "InAppMessage"

    internal const val COLUMN_IAM_ROW_ID = "row_id"
    internal const val COLUMN_IAM_ID = "iamId"
    internal const val COLUMN_IAM_INSTANCE_ID = "iamInstanceId"
    internal const val COLUMN_IAM_DISPLAY_RULES = "iamDisplayRules"
    internal const val COLUMN_IAM_LAST_SHOW_TIME = "iamLastShowTime"
    internal const val COLUMN_IAM_SHOW_COUNT = "iamShowCount"
    internal const val COLUMN_IAM_LAYOUT_TYPE = "iamLayoutType"
    internal const val COLUMN_IAM_MODEL = "iamModel"
    internal const val COLUMN_IAM_POSITION = "iamPosition"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME" +
                "(" +
                "$COLUMN_IAM_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_IAM_ID INTEGER NOT NULL, " +
                "$COLUMN_IAM_INSTANCE_ID INTEGER NOT NULL, " +
                "$COLUMN_IAM_DISPLAY_RULES TEXT NOT NULL, " +
                "$COLUMN_IAM_LAST_SHOW_TIME INTEGER, " +
                "$COLUMN_IAM_SHOW_COUNT INTEGER NOT NULL, " +
                "$COLUMN_IAM_LAYOUT_TYPE TEXT, " +
                "$COLUMN_IAM_MODEL TEXT, " +
                "$COLUMN_IAM_POSITION TEXT" +
                ")"

    internal const val SQL_UPGRADE_TABLE_VERSION_9 = "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IAM_POSITION TEXT"

    internal object SegmentSchema {
        internal const val TABLE_NAME_SEGMENT = "Segment"

        internal const val COLUMN_SEGMENT_ID = "segmentId"
        internal const val COLUMN_IS_IN_SEGMENT = "isInSegment"
        internal const val COLUMN_LAST_CHECK_TIME = "lastCheckTime"
        internal const val COLUMN_CHECK_STATUS_CODE = "checkStatusCode"
        internal const val COLUMN_RETRY_AFTER = "retryAfter"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_SEGMENT" +
                    "(" +
                    "${InAppMessageSchema.COLUMN_IAM_ROW_ID} INTEGER NOT NULL, " +
                    "$COLUMN_SEGMENT_ID INTEGER NOT NULL, " +
                    "$COLUMN_IS_IN_SEGMENT TEXT, " +
                    "$COLUMN_LAST_CHECK_TIME INTEGER, " +
                    "$COLUMN_CHECK_STATUS_CODE INTEGER, " +
                    "$COLUMN_RETRY_AFTER INTEGER, " +
                    "FOREIGN KEY (${InAppMessageSchema.COLUMN_IAM_ROW_ID}) REFERENCES ${InAppMessageSchema.TABLE_NAME} (${InAppMessageSchema.COLUMN_IAM_ROW_ID}) ON DELETE CASCADE" +
                    ")"
    }

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_IAM_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_IAM_ID,
        COLUMN_IAM_INSTANCE_ID,
        COLUMN_IAM_DISPLAY_RULES,
        COLUMN_IAM_LAST_SHOW_TIME,
        COLUMN_IAM_SHOW_COUNT,
        COLUMN_IAM_LAYOUT_TYPE,
        COLUMN_IAM_MODEL,
        COLUMN_IAM_POSITION
    )
}