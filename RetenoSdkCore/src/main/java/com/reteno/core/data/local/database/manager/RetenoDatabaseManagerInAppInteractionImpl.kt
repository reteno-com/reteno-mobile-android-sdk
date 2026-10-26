package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.util.getInAppInteraction
import com.reteno.core.data.local.database.util.putInAppInteraction
import com.reteno.core.data.local.model.interaction.InAppInteractionDb

internal class RetenoDatabaseManagerInAppInteractionImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerInAppInteraction {

    override fun insertInteraction(interaction: InAppInteractionDb) {
        database.insert(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            contentValues = ContentValues().apply { putInAppInteraction(interaction) }
        )
    }

    override fun getInteractions(limit: Int?): List<InAppInteractionDb> = readInteractions {
        query(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            columns = InAppInteractionSchema.getAllColumns(),
            orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
            limit = limit?.toString()
        )
    }

    override fun getInAppInteractionsCount(): Long =
        database.getRowCount(InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION)

    override fun deleteInteraction(interaction: InAppInteractionDb): Boolean = database.delete(
        table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
        whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
        whereArgs = arrayOf(interaction.rowId)
    ) > 0

    override fun deleteInteractionsByTime(outdatedTime: String): List<InAppInteractionDb> {
        val outdated = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME} < ?"
        val interactions = readInteractions {
            query(
                table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                columns = InAppInteractionSchema.getAllColumns(),
                selection = outdated,
                selectionArgs = arrayOf(outdatedTime)
            )
        }
        database.delete(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            whereClause = outdated,
            whereArgs = arrayOf(outdatedTime)
        )
        return interactions
    }

    private fun readInteractions(query: RetenoDatabase.() -> Cursor) = database.readRows(
        table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
        rowIdColumn = InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID,
        query = query,
        read = { getInAppInteraction() }
    )
}
