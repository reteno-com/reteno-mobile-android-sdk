package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.model.interaction.InAppInteractionDb

fun ContentValues.putInAppInteraction(inAppInteractionDb: InAppInteractionDb) {
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID, inAppInteractionDb.interactionId)
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME, inAppInteractionDb.time)
    put(InAppInteractionSchema.COLUMN_IN_APP_INSTANCE_ID, inAppInteractionDb.messageInstanceId)
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS, inAppInteractionDb.status)
    put(InAppInteractionSchema.COLUMN_IN_APP_STATUS_DESCRIPTION, inAppInteractionDb.statusDescription)
    put(InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT, inAppInteractionDb.workflowContext)
    put(InAppInteractionSchema.COLUMN_DELIVERED, if (inAppInteractionDb.delivered) 1 else 0)
    put(InAppInteractionSchema.COLUMN_ATTEMPTS, inAppInteractionDb.attempts)
    put(InAppInteractionSchema.COLUMN_NEXT_ATTEMPT, inAppInteractionDb.nextAttemptAt)
    put(InAppInteractionSchema.COLUMN_WORKFLOW_EXPIRY, inAppInteractionDb.workflowExpiresAt)
    put(InAppInteractionSchema.COLUMN_ACTION, inAppInteractionDb.action)
}

fun Cursor.getInAppInteraction(): InAppInteractionDb = InAppInteractionDb(
    rowId = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID)),
    interactionId = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID)),
    time = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME)),
    messageInstanceId = getLong(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INSTANCE_ID)),
    status = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS)),
    statusDescription = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_STATUS_DESCRIPTION)),
    workflowContext = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT)),
    delivered = getLong(getColumnIndex(InAppInteractionSchema.COLUMN_DELIVERED)) == 1L,
    attempts = getInt(getColumnIndex(InAppInteractionSchema.COLUMN_ATTEMPTS)),
    nextAttemptAt = getLong(getColumnIndex(InAppInteractionSchema.COLUMN_NEXT_ATTEMPT)),
    workflowExpiresAt = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_WORKFLOW_EXPIRY)),
    action = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_ACTION)),
)
