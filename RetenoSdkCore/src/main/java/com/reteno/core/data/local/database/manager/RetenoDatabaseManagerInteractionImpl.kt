package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InteractionRequestSchema
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.database.util.getInteraction
import com.reteno.core.data.local.database.util.getInteractionRequest
import com.reteno.core.data.local.database.util.putInteraction
import com.reteno.core.data.local.database.util.putInteractionRequest
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb

internal class RetenoDatabaseManagerInteractionImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerInteraction {

    override fun insertInteractionRequest(request: InteractionRequestDb) {
        database.insert(
            table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
            contentValues = ContentValues().apply { putInteractionRequest(request) }
        )
    }

    override fun getInteractionRequests(): List<InteractionRequestDb> = database.readRows(
        table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
        rowIdColumn = InteractionRequestSchema.COLUMN_INTERACTION_ROW_ID,
        query = {
            query(
                table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
                columns = InteractionRequestSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC"
            )
        },
        read = { getInteractionRequest() }
    )

    override fun deleteInteractionRequest(interaction: InteractionRequestDb): Boolean = database.delete(
        table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
        whereClause = "${InteractionRequestSchema.COLUMN_INTERACTION_ROW_ID}=?",
        whereArgs = arrayOf(interaction.rowId)
    ) > 0

    override fun insertInteraction(interaction: InteractionDb) {
        database.insert(
            table = InteractionSchema.TABLE_NAME,
            contentValues = ContentValues().apply { putInteraction(interaction) }
        )
    }

    override fun getInteractions(limit: Int?): List<InteractionDb> = readInteractions {
        query(
            table = InteractionSchema.TABLE_NAME,
            columns = InteractionSchema.getAllColumns(),
            orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
            limit = limit?.toString()
        )
    }

    override fun getInteractionCount(): Long = database.getRowCount(InteractionSchema.TABLE_NAME)

    override fun deleteInteraction(interaction: InteractionDb): Boolean = database.delete(
        table = InteractionSchema.TABLE_NAME,
        whereClause = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?",
        whereArgs = arrayOf(interaction.rowId)
    ) > 0

    override fun deleteInteractionByTime(outdatedTime: String): List<InteractionDb> {
        val outdated = "${InteractionSchema.COLUMN_INTERACTION_TIME} < ?"
        val interactions = readInteractions {
            query(
                table = InteractionSchema.TABLE_NAME,
                columns = InteractionSchema.getAllColumns(),
                selection = outdated,
                selectionArgs = arrayOf(outdatedTime)
            )
        }
        database.delete(table = InteractionSchema.TABLE_NAME, whereClause = outdated, whereArgs = arrayOf(outdatedTime))
        return interactions
    }

    private fun readInteractions(query: RetenoDatabase.() -> Cursor) = database.readRows(
        table = InteractionSchema.TABLE_NAME,
        rowIdColumn = InteractionSchema.COLUMN_INTERACTION_ROW_ID,
        query = query,
        read = { getInteraction() }
    )
}
