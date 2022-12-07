package com.reteno.core.data.local.database.schema

internal object InteractionSchema {
    internal const val TABLE_NAME_INTERACTION = "Interaction"

    internal const val COLUMN_INTERACTION_ROW_ID = "row_id"

    internal const val COLUMN_INTERACTION_ID = "interactionId"
    internal const val COLUMN_INTERACTION_STATUS = "status"
    internal const val COLUMN_INTERACTION_TIME = "time"
    internal const val COLUMN_INTERACTION_TOKEN = "token"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_INTERACTION" +
                "(" +
                "$COLUMN_INTERACTION_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_INTERACTION_ID TEXT, " +
                "$COLUMN_INTERACTION_STATUS TEXT, " +
                "$COLUMN_INTERACTION_TIME TEXT, " +
                "$COLUMN_INTERACTION_TOKEN TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_INTERACTION_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_INTERACTION_ID,
        COLUMN_INTERACTION_STATUS,
        COLUMN_INTERACTION_TIME,
        COLUMN_INTERACTION_TOKEN
    )
}