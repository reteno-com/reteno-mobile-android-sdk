package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.util.allElementsNotNull

fun ContentValues.putInAppInteraction(inAppInteractionDb: InAppInteractionDb) {
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID, inAppInteractionDb.interactionId)
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME, inAppInteractionDb.time)
    put(InAppInteractionSchema.COLUMN_IN_APP_INSTANCE_ID, inAppInteractionDb.messageInstanceId)
    put(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS, inAppInteractionDb.status)
    put(InAppInteractionSchema.COLUMN_IN_APP_STATUS_DESCRIPTION, inAppInteractionDb.statusDescription)
}

fun Cursor.getInAppInteraction(): InAppInteractionDb? {
    val rowId = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID))
    val interactionId = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID))
    val time = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME))
    val messageInstanceId = getLongOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INSTANCE_ID))
    val status = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS))
    val statusDescription = getStringOrNull(getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_STATUS_DESCRIPTION))

    return if (interactionId != null && time != null && messageInstanceId != null && status != null) {
        InAppInteractionDb(
            rowId = rowId,
            interactionId = interactionId,
            time = time,
            messageInstanceId = messageInstanceId,
            status = status,
            statusDescription = statusDescription,
        )
    } else {
        null
    }
}