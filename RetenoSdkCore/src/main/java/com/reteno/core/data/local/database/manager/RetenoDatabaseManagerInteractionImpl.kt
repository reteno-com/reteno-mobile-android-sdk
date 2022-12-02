package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.database.util.getInteraction
import com.reteno.core.data.local.database.util.putInteraction
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.SQLException

internal class RetenoDatabaseManagerInteractionImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerInteraction {

    private val contentValues = ContentValues()

    override fun insertInteraction(interaction: InteractionDb) {
        contentValues.putInteraction(interaction)
        database.insert(table = InteractionSchema.TABLE_NAME_INTERACTION, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getInteractions(limit: Int?): List<InteractionDb> {
        val interactionEvents: MutableList<InteractionDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = InteractionSchema.TABLE_NAME_INTERACTION,
                columns = InteractionSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val interaction = cursor.getInteraction()

                if (interaction != null) {
                    interactionEvents.add(interaction)
                } else {
                    val rowId =
                        cursor.getLongOrNull(cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getInteractions(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InteractionSchema.TABLE_NAME_INTERACTION,
                            whereClause = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getInteractions(). Removed invalid entry from database. interaction=$interaction ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Interactions from the table", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return interactionEvents
    }

    override fun getInteractionCount(): Long = database.getRowCount(InteractionSchema.TABLE_NAME_INTERACTION)

    override fun deleteInteractions(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        database.delete(
            table = InteractionSchema.TABLE_NAME_INTERACTION,
            whereClause = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID} in (select ${InteractionSchema.COLUMN_INTERACTION_ROW_ID} from ${InteractionSchema.TABLE_NAME_INTERACTION} ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order LIMIT $count)"
        )
    }

    override fun deleteInteractionByTime(outdatedTime: String): Int {
        return database.delete(
            table = InteractionSchema.TABLE_NAME_INTERACTION,
            whereClause = "${InteractionSchema.COLUMN_INTERACTION_TIME} < '$outdatedTime'"
        )
    }

    companion object {
        val TAG: String = RetenoDatabaseManagerInteractionImpl::class.java.simpleName
    }
}