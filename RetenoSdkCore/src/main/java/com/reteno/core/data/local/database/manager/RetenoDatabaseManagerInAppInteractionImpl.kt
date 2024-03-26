package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.util.getInAppInteraction
import com.reteno.core.data.local.database.util.putInAppInteraction
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerInAppInteractionImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerInAppInteraction {

    private val contentValues = ContentValues()

    override fun insertInteraction(interaction: InAppInteractionDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertInteraction(): ", "inAppInteraction = [", interaction, "]")
        /*@formatter:on*/
        contentValues.putInAppInteraction(interaction)
        database.insert(table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getInteractions(limit: Int?): List<InAppInteractionDb> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppInteractions(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val interactions: MutableList<InAppInteractionDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                columns = InAppInteractionSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val interaction = cursor.getInAppInteraction()

                if (interaction != null) {
                    interactions.add(interaction)
                } else {
                    val rowId =
                        cursor.getLongOrNull(cursor.getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getInteractions(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                            whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
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
        return interactions
    }

    override fun getInAppInteractionsCount(): Long = database.getRowCount(InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION)

    override fun deleteInteraction(interaction: InAppInteractionDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteInteraction(): ", "interaction = [", interaction, "]")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
            whereArgs = arrayOf(interaction.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteInteractionsByTime(outdatedTime: String): List<InAppInteractionDb> {
        /*@formatter:off*/ Logger.i(TAG, "deleteInteractionByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/

        val whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME} < '$outdatedTime'"

        var cursor: Cursor? = null
        val interactionsList = mutableListOf<InAppInteractionDb>()
        try {
            cursor = database.query(
                table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                columns = InAppInteractionSchema.getAllColumns(),
                selection = whereClause
            )

            while (cursor.moveToNext()) {
                val interaction = cursor.getInAppInteraction()
                if (interaction != null) {
                    interactionsList.add(interaction)
                } else {
                    val rowId = cursor.getLongOrNull(
                        cursor.getColumnIndex(InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID)
                    )
                    val exception =
                        SQLException("deleteInteractionByTime() Unable to read data from SQL database. interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "deleteInteractionByTime(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                            whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "deleteInteractionByTime(). Removed invalid entry from database. interaction=$interaction ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "deleteInteractionByTime() handleSQLiteError(): Unable to get Interactions from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        database.delete(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            whereClause = whereClause
        )

        return interactionsList
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerInAppInteractionImpl::class.java.simpleName
    }
}