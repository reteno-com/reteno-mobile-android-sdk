package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.util.getInAppInteraction
import com.reteno.core.data.local.database.util.putInAppInteraction
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.util.Logger
import java.time.Instant

internal class RetenoDatabaseManagerInAppInteractionImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerInAppInteraction {

    @Synchronized
    override fun insertInteraction(interaction: InAppInteractionDb) {
        val context = interaction.workflowContext
        if (context != null && interaction.status == "OPENED" && isWorkflowShown(context, interaction.interactionId)) {
            return
        }
        val inserted = database.insert(
            table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            contentValues = ContentValues().apply { putInAppInteraction(interaction) }
        )
        if (context != null) {
            check(inserted != -1L) { "Failed to persist In-app observation" }
        }
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
        database.getRowCount(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            "${InAppInteractionSchema.COLUMN_DELIVERED}=0"
        )

    override fun deleteInteraction(interaction: InAppInteractionDb): Boolean {
        if (interaction.workflowContext != null && interaction.status in listOf("OPENED", "CLICKED")) {
            return database.update(
                InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                ContentValues().apply { put(InAppInteractionSchema.COLUMN_DELIVERED, 1) },
                "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
                arrayOf(interaction.rowId)
            ) > 0
        }

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
            val outdated = runCatching {
                Instant.parse(observation.time).isBefore(Instant.parse(outdatedTime))
            }.getOrDefault(false)
            if (expired && (observation.delivered || outdated)) {
                database.delete(
                    InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
                    "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
                    arrayOf(observation.rowId)
                )
            } else if (outdated && !observation.delivered) {
                deleteInteraction(observation)
                Logger.captureMessage("Workflow observation retention exhausted", LogLevel.ERROR)
            }
        }
        val outdated = "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_TIME} < ? AND " +
            "${InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT} IS NULL"
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

    override fun recordAttempt(interaction: InAppInteractionDb) {
        val updated = database.update(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            ContentValues().apply {
                put(InAppInteractionSchema.COLUMN_ATTEMPTS, interaction.attempts + 1)
                put(
                    InAppInteractionSchema.COLUMN_NEXT_ATTEMPT,
                    System.currentTimeMillis() + (30_000L shl interaction.attempts.coerceAtMost(10))
                )
            },
            "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
            arrayOf(interaction.rowId)
        )
        check(updated > 0) { "Failed to persist workflow retry attempt" }
    }

    override fun deferUntil(interaction: InAppInteractionDb, nextAttemptAt: Long) {
        database.update(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            ContentValues().apply { put(InAppInteractionSchema.COLUMN_NEXT_ATTEMPT, nextAttemptAt) },
            "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID}=?",
            arrayOf(interaction.rowId)
        )
    }

    override fun isWorkflowShown(context: String, iid: String): Boolean =
        database.getRowCount(
            InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
            "${InAppInteractionSchema.COLUMN_WORKFLOW_CONTEXT}=? AND " +
                "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ID}=? AND " +
                "${InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_STATUS} IN (?, ?)",
            arrayOf(context, iid, "OPENED", "CLICKED")
        ) > 0

    override fun getPendingInteractions(context: String): List<InAppInteractionDb> =
        getInteractions().filter {
            !it.delivered && it.nextAttemptAt <= System.currentTimeMillis() &&
                (it.workflowContext == null || it.workflowContext == context)
        }

    private fun readInteractions(query: RetenoDatabase.() -> Cursor) = database.readRows(
        table = InAppInteractionSchema.TABLE_NAME_IN_APP_INTERACTION,
        rowIdColumn = InAppInteractionSchema.COLUMN_IN_APP_INTERACTION_ROW_ID,
        query = query,
        read = { getInAppInteraction() }
    )
}
