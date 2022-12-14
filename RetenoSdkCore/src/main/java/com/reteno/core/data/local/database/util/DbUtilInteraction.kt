package com.reteno.core.data.local.database.util

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.util.allElementsNotNull
import net.sqlcipher.Cursor

fun ContentValues.putInteraction(interaction: InteractionDb) {
    put(InteractionSchema.COLUMN_INTERACTION_ID, interaction.interactionId)
    put(InteractionSchema.COLUMN_INTERACTION_TIME, interaction.time)
    put(InteractionSchema.COLUMN_INTERACTION_STATUS, interaction.status.toString())
    put(InteractionSchema.COLUMN_INTERACTION_TOKEN, interaction.token)
}

fun Cursor.getInteraction(): InteractionDb? {
    val interactionId = getStringOrNull(getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ID))
    val status = getStringOrNull(getColumnIndex(InteractionSchema.COLUMN_INTERACTION_STATUS))
    val time = getStringOrNull(getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TIME))
    val token = getStringOrNull(getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TOKEN))

    return if (allElementsNotNull(interactionId, status, time, token)) {
        InteractionDb(
            interactionId = interactionId!!,
            status = InteractionStatusDb.fromString(status),
            time = time!!,
            token = token!!
        )
    } else {
        null
    }
}