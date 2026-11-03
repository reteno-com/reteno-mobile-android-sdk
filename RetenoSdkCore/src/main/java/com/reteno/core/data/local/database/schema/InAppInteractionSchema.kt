package com.reteno.core.data.local.database.schema

internal object InAppInteractionSchema {
    internal const val TABLE_NAME_IN_APP_INTERACTION = "InAppInteraction"

    internal const val COLUMN_IN_APP_INTERACTION_ROW_ID = "row_id"

    internal const val COLUMN_IN_APP_INTERACTION_ID = "interactionId"
    internal const val COLUMN_IN_APP_INTERACTION_TIME = "time"
    internal const val COLUMN_IN_APP_INSTANCE_ID = "instanceId"
    internal const val COLUMN_IN_APP_INTERACTION_STATUS = "status"
    internal const val COLUMN_IN_APP_STATUS_DESCRIPTION = "statusDescription"

    internal const val COLUMN_ACTION = "action"
    internal const val SQL_UPGRADE_ACTION = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_ACTION TEXT"
    internal const val COLUMN_WORKFLOW_CONTEXT = "workflowContext"
    internal const val COLUMN_DELIVERED = "delivered"
    internal const val COLUMN_WORKFLOW_EXPIRY = "workflowExpiry"
    internal const val SQL_UPGRADE_EXPIRY = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_WORKFLOW_EXPIRY TEXT"
    internal const val COLUMN_NEXT_ATTEMPT = "nextAttemptAt"
    internal const val SQL_UPGRADE_NEXT_ATTEMPT = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_NEXT_ATTEMPT INTEGER NOT NULL DEFAULT 0"
    internal const val COLUMN_ATTEMPTS = "attempts"
    internal const val SQL_UPGRADE_ATTEMPTS = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_ATTEMPTS INTEGER NOT NULL DEFAULT 0"
    internal const val SQL_UPGRADE_CONTEXT = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_WORKFLOW_CONTEXT TEXT"
    internal const val SQL_UPGRADE_DELIVERED = "ALTER TABLE $TABLE_NAME_IN_APP_INTERACTION ADD COLUMN $COLUMN_DELIVERED INTEGER NOT NULL DEFAULT 0"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_IN_APP_INTERACTION" +
                "(" +
                "$COLUMN_IN_APP_INTERACTION_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_IN_APP_INTERACTION_ID TEXT NOT NULL, " +
                "$COLUMN_IN_APP_INTERACTION_TIME TEXT NOT NULL, " +
                "$COLUMN_IN_APP_INSTANCE_ID INTEGER NOT NULL, " +
                "$COLUMN_IN_APP_INTERACTION_STATUS TEXT NOT NULL, " +
                "$COLUMN_IN_APP_STATUS_DESCRIPTION TEXT, " +
                "$COLUMN_WORKFLOW_CONTEXT TEXT, " +
                "$COLUMN_DELIVERED INTEGER NOT NULL DEFAULT 0, " +
                "$COLUMN_ATTEMPTS INTEGER NOT NULL DEFAULT 0, " +
                "$COLUMN_WORKFLOW_EXPIRY TEXT, " +
                "$COLUMN_NEXT_ATTEMPT INTEGER NOT NULL DEFAULT 0, " +
                "$COLUMN_ACTION TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_IN_APP_INTERACTION_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_IN_APP_INTERACTION_ID,
        COLUMN_IN_APP_INTERACTION_TIME,
        COLUMN_IN_APP_INSTANCE_ID,
        COLUMN_IN_APP_INTERACTION_STATUS,
        COLUMN_IN_APP_STATUS_DESCRIPTION,
        COLUMN_WORKFLOW_CONTEXT,
        COLUMN_DELIVERED,
        COLUMN_ATTEMPTS,
        COLUMN_NEXT_ATTEMPT,
        COLUMN_WORKFLOW_EXPIRY,
        COLUMN_ACTION,
    )
}