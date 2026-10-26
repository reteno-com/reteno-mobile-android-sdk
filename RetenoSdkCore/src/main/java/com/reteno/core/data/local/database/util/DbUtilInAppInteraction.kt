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
}

fun Cursor.getInAppInteraction(): InAppInteractionDb = InAppInteractionDb(
    rowId = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID)),
    interactionId = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID)),
    time = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME)),
    messageInstanceId = getLong(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INSTANCE_ID)),
    status = getString(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS)),
    statusDescription = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_STATUS_DESCRIPTION)),
)
