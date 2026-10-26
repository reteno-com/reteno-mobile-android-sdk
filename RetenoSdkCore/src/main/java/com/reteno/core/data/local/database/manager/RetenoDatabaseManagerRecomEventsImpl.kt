package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.database.util.getRecomEvent
import com.reteno.core.data.local.database.util.putRecomVariantId
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerRecomEventsImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerRecomEvents {

    override fun insertRecomEvents(recomEvents: RecomEventsDb) {
        val events = recomEvents.recomEvents.orEmpty()
        if (events.isEmpty()) {
            Logger.e(TAG, "insertRecomEvents(): ", IllegalArgumentException("no recommendation events to save"))
            return
        }
        val variantId = recomEvents.recomVariantId
        if (!isVariantStored(variantId) && !storeVariant(variantId)) return
        database.insertMultiple(
            table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
            contentValues = events.toContentValuesList(variantId)
        )
    }

    private fun isVariantStored(variantId: String): Boolean = database.getRowCount(
        RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
        whereClause = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
        whereArgs = arrayOf(variantId)
    ) > 0

    private fun storeVariant(variantId: String): Boolean = database.insert(
        table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
        contentValues = ContentValues().apply { putRecomVariantId(variantId) }
    ) != -1L

    override fun getRecomEvents(limit: Int?): List<RecomEventsDb> {
        val variantIds = database.readRows(
            table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
            rowIdColumn = RecomEventsSchema.COLUMN_RECOM_VARIANT_ID,
            query = {
                query(table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS, columns = RecomEventsSchema.getAllColumns())
            },
            read = { getString(getColumnIndexOrThrow(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID)) }
        )
        val recomEvents = variantIds.mapNotNull { variantId ->
            val events = database.readRows(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                rowIdColumn = RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID,
                query = {
                    query(
                        table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                        columns = RecomEventsSchema.RecomEventSchema.getAllColumns(),
                        selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                        selectionArgs = arrayOf(variantId),
                        limit = limit?.toString()
                    )
                },
                read = { getRecomEvent() }
            )
            if (events.isEmpty()) null else RecomEventsDb(recomVariantId = variantId, recomEvents = events)
        }
        database.cleanUnlinkedRecomVariantIds()
        return recomEvents
    }

    override fun getRecomEventsCount(): Long =
        database.getRowCount(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT)

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedRecomVariantIds] each time you remove events from RecomEvent table (Child table)
     */
    override fun deleteRecomEvents(recomEvents: List<RecomEventsDb>) {
        recomEvents.flatMap { it.recomEvents.orEmpty() }.mapNotNull { it.rowId }.forEach { rowId ->
            database.delete(
                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                whereClause = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
        database.cleanUnlinkedRecomVariantIds()
    }

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedRecomVariantIds] each time you remove events from RecomEvent table (Child table)
     */
    override fun deleteRecomEventsByTime(outdatedTime: String): Int {
        val count = database.delete(
            table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
            whereClause = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED} < ?",
            whereArgs = arrayOf(outdatedTime)
        )
        database.cleanUnlinkedRecomVariantIds()
        return count
    }

    private companion object {
        const val TAG = "RetenoDatabaseManagerRecomEventsImpl"
    }
}
