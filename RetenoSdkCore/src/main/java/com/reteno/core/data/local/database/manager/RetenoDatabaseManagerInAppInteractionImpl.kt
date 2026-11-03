package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import java.time.Instant
import com.reteno.core.domain.model.logevent.LogLevel
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


    @Synchronized
    override fun insertInteraction(interaction: InAppInteractionDb) {
        if (interaction.workflowContext != null && interaction.status == "OPENED" &&
            isWorkflowShown(interaction.workflowContext, interaction.interactionId)) return
        Logger.d(TAG, "insertInteraction(): ")
        /*@formatter:on*/
        val contentValues = ContentValues()
        contentValues.putInAppInteraction(interaction)
        check(database.insert(table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION, contentValues = contentValues) != -1L) {
            "Failed to persist In-app observation"
        }
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

    override fun recordAttempt(interaction: InAppInteractionDb) {
        check(database.update(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            ContentValues().apply {
                put(InAppInteractionSchema.COLUMN_ATTEMPTS, interaction.attempts + 1)
                put(InAppInteractionSchema.COLUMN_NEXT_ATTEMPT, System.currentTimeMillis() + (30_000L shl interaction.attempts.coerceAtMost(10)))
            },
            "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
            arrayOf(interaction.rowId)
        ) > 0) { "Failed to persist workflow retry attempt" }
    }

    override fun deferUntil(interaction: InAppInteractionDb, nextAttemptAt: Long) {
        database.update(InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            ContentValues().apply { put(InAppInteractionSchema.COLUMN_NEXT_ATTEMPT, nextAttemptAt) },
            "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?", arrayOf(interaction.rowId))
    }

    override fun isWorkflowShown(context: String, iid: String): Boolean =
        database.getRowCount(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            "${InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT}=? AND ${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID}=? AND ${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS} IN (?, ?)",
            arrayOf(context, iid, "OPENED", "CLICKED")
        ) > 0

    override fun getPendingInteractions(context: String): List<InAppInteractionDb> =
        getInteractions().filter { !it.delivered && it.nextAttemptAt <= System.currentTimeMillis() && (it.workflowContext == null || it.workflowContext == context) }

    override fun deleteInteraction(interaction: InAppInteractionDb): Boolean {
        // Retain visible observations as shown markers after acknowledgment or a terminal response.
        if (interaction.workflowContext != null && interaction.status in listOf("OPENED", "CLICKED")) {
            return database.update(
                InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                ContentValues().apply { put(InAppInteractionSchema.COLUMN_DELIVERED, 1) },
                "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
                arrayOf(interaction.rowId)
            ) > 0
        }
        Logger.d(TAG, "deleteInteraction(): ")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
            whereArgs = arrayOf(interaction.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteInteractionsByTime(outdatedTime: String): List<InAppInteractionDb> {
        val now = Instant.now()
        getInteractions().filter { it.workflowContext != null }.forEach { observation ->
            val expired = runCatching { !now.isBefore(Instant.parse(observation.workflowExpiresAt)) }.getOrDefault(false)
            val outdated = runCatching { Instant.parse(observation.time).isBefore(Instant.parse(outdatedTime)) }.getOrDefault(false)
            if (expired && (observation.delivered || outdated)) {
                database.delete(InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                    "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?", arrayOf(observation.rowId))
            } else if (outdated && !observation.delivered) {
                deleteInteraction(observation)
                Logger.captureMessage("Workflow observation retention exhausted", LogLevel.ERROR)
            }
        }
        /*@formatter:off*/ Logger.i(TAG, "deleteInteractionByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/

        val whereClause = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME} < '$outdatedTime' AND ${InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT} IS NULL"

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
