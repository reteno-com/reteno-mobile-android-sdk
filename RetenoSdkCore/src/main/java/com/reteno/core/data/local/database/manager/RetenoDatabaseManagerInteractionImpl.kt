package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
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
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerInteractionImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerInteraction {

    override fun insertInteractionRequest(request: InteractionRequestDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertInteractionRequest(): ", "request = [", request, "]")
        /*@formatter:on*/
        val contentValues = ContentValues()
        contentValues.putInteractionRequest(request)
        database.insert(
            table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
            contentValues = contentValues
        )
        contentValues.clear()
    }

    override fun getInteractionRequests(): List<InteractionRequestDb> {
        /*@formatter:off*/ Logger.i(TAG, "getInteractionRequests(): ")
        /*@formatter:on*/

        val interactions: MutableList<InteractionRequestDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
                columns = InteractionRequestSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC"
            )
            while (cursor.moveToNext()) {
                val timestamp =
                    cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val interaction = cursor.getInteractionRequest()

                if (interaction != null) {
                    interactions.add(interaction)
                } else {
                    val rowId =
                        cursor.getLongOrNull(cursor.getColumnIndex(InteractionRequestSchema.COLUMN_INTERACTION_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getInteractionRequests(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
                            whereClause = "${InteractionRequestSchema.COLUMN_INTERACTION_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getInteractionRequests(). Removed invalid entry from database. interaction=$interaction ", exception)
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

    override fun deleteInteractionRequest(interaction: InteractionRequestDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteInteractionRequest(): ", "request = [", interaction, "]")
        /*@formatter:on*/
        val removedRecordsCount = database.delete(
            table = InteractionRequestSchema.TABLE_NAME_INTERACTION_REQUEST,
            whereClause = "${InteractionRequestSchema.COLUMN_INTERACTION_ROW_ID}=?",
            whereArgs = arrayOf(interaction.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun insertInteraction(interaction: InteractionDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertInteraction(): ", "interaction = [", interaction, "]")
        /*@formatter:on*/
        val contentValues = ContentValues()
        contentValues.putInteraction(interaction)
        database.insert(
            table = InteractionSchema.TABLE_NAME_INTERACTION,
            contentValues = contentValues
        )
        contentValues.clear()
    }

    override fun getInteractions(limit: Int?): List<InteractionDb> {
        /*@formatter:off*/ Logger.i(TAG, "getInteractions(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val interactions: MutableList<InteractionDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = InteractionSchema.TABLE_NAME_INTERACTION,
                columns = InteractionSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp =
                    cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val interaction = cursor.getInteraction()

                if (interaction != null) {
                    interactions.add(interaction)
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
        return interactions
    }

    override fun getInteractionCount(): Long =
        database.getRowCount(InteractionSchema.TABLE_NAME_INTERACTION)

    override fun deleteInteraction(interaction: InteractionDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteInteraction(): ", "interaction = [", interaction, "]")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = InteractionSchema.TABLE_NAME_INTERACTION,
            whereClause = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?",
            whereArgs = arrayOf(interaction.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteInteractionByTime(outdatedTime: String): List<InteractionDb> {
        /*@formatter:off*/ Logger.i(TAG, "deleteInteractionByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/

        val whereClause = "${InteractionSchema.COLUMN_INTERACTION_TIME} < '$outdatedTime'"

        var cursor: Cursor? = null
        val interactionsList = mutableListOf<InteractionDb>()
        try {
            cursor = database.query(
                table = InteractionSchema.TABLE_NAME_INTERACTION,
                columns = InteractionSchema.getAllColumns(),
                selection = whereClause
            )

            while (cursor.moveToNext()) {
                val interaction = cursor.getInteraction()
                if (interaction != null) {
                    interactionsList.add(interaction)
                } else {
                    val rowId = cursor.getLongOrNull(
                        cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ROW_ID)
                    )
                    val exception =
                        SQLException("deleteInteractionByTime() Unable to read data from SQL database. interaction=$interaction")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "deleteInteractionByTime(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InteractionSchema.TABLE_NAME_INTERACTION,
                            whereClause = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?",
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
            table = InteractionSchema.TABLE_NAME_INTERACTION,
            whereClause = whereClause
        )

        return interactionsList
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerInteractionImpl::class.java.simpleName
    }
}