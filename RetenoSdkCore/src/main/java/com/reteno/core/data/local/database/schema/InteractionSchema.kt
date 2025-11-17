package com.reteno.core.data.local.database.schema

internal object InteractionSchema {
    internal const val TABLE_NAME = "Interaction"

    internal const val COLUMN_INTERACTION_ROW_ID = "row_id"

    internal const val COLUMN_INTERACTION_ID = "interactionId"
    internal const val COLUMN_INTERACTION_STATUS = "status"
    internal const val COLUMN_INTERACTION_TIME = "time"
    internal const val COLUMN_INTERACTION_TOKEN = "token"
    internal const val COLUMN_INTERACTION_ACTION = "action"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME" +
                "(" +
                "$COLUMN_INTERACTION_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_INTERACTION_ID TEXT, " +
                "$COLUMN_INTERACTION_STATUS TEXT, " +
                "$COLUMN_INTERACTION_TIME TEXT, " +
                "$COLUMN_INTERACTION_TOKEN TEXT, " +
                "$COLUMN_INTERACTION_ACTION TEXT" +
                ")"

    internal const val SQL_UPGRADE_TABLE_VERSION_4 =
        "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_INTERACTION_ACTION TEXT"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_INTERACTION_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_INTERACTION_ID,
        COLUMN_INTERACTION_STATUS,
        COLUMN_INTERACTION_TIME,
        COLUMN_INTERACTION_TOKEN,
        COLUMN_INTERACTION_ACTION,
    )
}