package com.reteno.core.data.local.database.schema

internal object InAppInteractionSchema {
    internal const val TABLE_NAME_IN_APP_INTERACTION = "InAppInteraction"

    internal const val COLUMN_IN_APP_INTERACTION_ROW_ID = "row_id"

    internal const val COLUMN_IN_APP_INTERACTION_ID = "interactionId"
    internal const val COLUMN_IN_APP_INTERACTION_TIME = "time"
    internal const val COLUMN_IN_APP_INSTANCE_ID = "instanceId"
    internal const val COLUMN_IN_APP_INTERACTION_STATUS = "status"
    internal const val COLUMN_IN_APP_STATUS_DESCRIPTION = "statusDescription"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_IN_APP_INTERACTION" +
                "(" +
                "$COLUMN_IN_APP_INTERACTION_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_IN_APP_INTERACTION_ID TEXT NOT NULL, " +
                "$COLUMN_IN_APP_INTERACTION_TIME TEXT NOT NULL, " +
                "$COLUMN_IN_APP_INSTANCE_ID INTEGER NOT NULL, " +
                "$COLUMN_IN_APP_INTERACTION_STATUS TEXT NOT NULL, " +
                "$COLUMN_IN_APP_STATUS_DESCRIPTION TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_IN_APP_INTERACTION_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_IN_APP_INTERACTION_ID,
        COLUMN_IN_APP_INTERACTION_TIME,
        COLUMN_IN_APP_INSTANCE_ID,
        COLUMN_IN_APP_INTERACTION_STATUS,
        COLUMN_IN_APP_STATUS_DESCRIPTION,
    )
}