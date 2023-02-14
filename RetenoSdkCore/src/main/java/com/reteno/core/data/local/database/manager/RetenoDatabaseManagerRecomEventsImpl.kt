package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.database.util.getRecomEvent
import com.reteno.core.data.local.database.util.putRecomVariantId
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.SQLException

internal class RetenoDatabaseManagerRecomEventsImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerRecomEvents {

    private val contentValues = ContentValues()

    override fun insertRecomEvents(recomEvents: RecomEventsDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertRecomEvents(): ", "INSERT")
        /*@formatter:on*/

        if (recomEvents.recomEvents == null || recomEvents.recomEvents.isEmpty()) {
            /*@formatter:off*/ Logger.e(TAG, "insertRecomEvents(): ", Throwable("recomEvents = $recomEvents"))
            /*@formatter:on*/
            return
        }

        val recomVariantId: String? =
            if (isRecomVariantIdPresentInParentTable(recomEvents.recomVariantId)) {
                recomEvents.recomVariantId
            } else {
                if (putRecomVariantIdToParentTable(recomEvents.recomVariantId)) {
                    recomEvents.recomVariantId
                } else {
                    null
                }
            }

        recomVariantId?.let { variantId ->
            insertRecomEventList(variantId, recomEvents.recomEvents)
        }
    }

    private fun isRecomVariantIdPresentInParentTable(recomVariantId: String): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "isRecomVariantIdPresentInParentTable(): ", "recomVariantId = [", recomVariantId, "]")
        /*@formatter:on*/

        var cursor: Cursor? = null

        try {
            cursor = database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns(),
                selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                selectionArgs = arrayOf(recomVariantId)
            )

            if (cursor.moveToFirst()) {
                return true
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get RecomEvents from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        return false
    }

    private fun putRecomVariantIdToParentTable(recomVariantId: String): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "putRecomVariantIdToParentTable(): ", "recomVariantId = [", recomVariantId, "]")
        /*@formatter:on*/

        contentValues.putRecomVariantId(recomVariantId)
        val parentRowId = database.insert(
            table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
            contentValues = contentValues
        )
        contentValues.clear()

        return parentRowId != -1L
    }

    private fun insertRecomEventList(
        variantId: String,
        recomEventListDb: List<RecomEventDb>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "insertRecomEventList(): ", "variantId = [", variantId, "], recomEventListDb = [", recomEventListDb, "]")
        /*@formatter:on*/

        val contentValues = recomEventListDb.toContentValuesList(variantId)
        database.insertMultiple(
            table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
            contentValues = contentValues
        )
    }

    override fun getRecomEvents(limit: Int?): List<RecomEventsDb> {
        /*@formatter:off*/ Logger.i(TAG, "getRecomEvents(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val recomVariantIds: MutableList<String> = readRecomVariantIds()
        val recomEventsResult: MutableList<RecomEventsDb> =
            readRecomEventList(recomVariantIds, limit)

        database.cleanUnlinkedRecomVariantIds()

        return recomEventsResult
    }

    private fun readRecomVariantIds(): MutableList<String> {
        /*@formatter:off*/ Logger.i(TAG, "readRecomVariantIds(): ", "")
        /*@formatter:on*/

        val recomVariantIds: MutableList<String> = mutableListOf()
        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = RecomEventsSchema.TABLE_NAME_RECOM_EVENTS,
                columns = RecomEventsSchema.getAllColumns()
            )
            while (cursor.moveToNext()) {
                val recomVariantId =
                    cursor.getStringOrNull(cursor.getColumnIndex(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID))

                if (recomVariantId != null) {
                    recomVariantIds.add(recomVariantId)
                } else {
                    val exception =
                        SQLException("Unable to read data from SQL database. recomVariantId=null")
                    /*@formatter:off*/ Logger.e(TAG, "Error reading database. recomVariantId=$recomVariantId", exception)
                    /*@formatter:on*/
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get RecomEvents from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        return recomVariantIds
    }

    private fun readRecomEventList(
        recomVariantIds: MutableList<String>,
        limit: Int?
    ): MutableList<RecomEventsDb> {
        val recomEventsResult: MutableList<RecomEventsDb> = mutableListOf()

        for (recomVariantId in recomVariantIds) {
            val recomEvents: MutableList<RecomEventDb> = mutableListOf()

            var cursorChild: Cursor? = null
            try {
                cursorChild = database.query(
                    table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                    columns = RecomEventsSchema.RecomEventSchema.getAllColumns(),
                    selection = "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}=?",
                    selectionArgs = arrayOf(recomVariantId),
                    limit = limit?.toString()
                )

                while (cursorChild.moveToNext()) {
                    val recomEvent = cursorChild.getRecomEvent()

                    if (recomEvent != null) {
                        recomEvents.add(recomEvent)
                    } else {
                        val rowId = cursorChild.getLongOrNull(
                            cursorChild.getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID)
                        )
                        val exception =
                            SQLException("Unable to read data from SQL database. recomEvent=$recomEvent")
                        if (rowId == null) {
                            /*@formatter:off*/ Logger.e(TAG, "getRecomEvents(). rowId is NULL ", exception)
                            /*@formatter:on*/
                        } else {
                            database.delete(
                                table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
                                whereClause = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_ROW_ID}=?",
                                whereArgs = arrayOf(rowId.toString())
                            )
                            /*@formatter:off*/ Logger.e(TAG, "getRecomEvents(). Removed invalid entry from database. recomEvent=$recomEvent ", exception)
                            /*@formatter:on*/
                        }
                    }
                }
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get RecomEvents from the table.", t)
                /*@formatter:on*/
            } finally {
                cursorChild?.close()
            }

            if (recomEvents.isNotEmpty()) {
                recomEventsResult.add(
                    RecomEventsDb(
                        recomVariantId = recomVariantId,
                        recomEvents = recomEvents
                    )
                )
            }
        }
        return recomEventsResult
    }

    override fun getRecomEventsCount(): Long =
        database.getRowCount(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT)

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedRecomVariantIds] each time you remove events from RecomEvent table (Child table)
     */
    override fun deleteRecomEvents(recomEvents: List<RecomEventsDb>) {
        /*@formatter:off*/ Logger.i(TAG, "deleteRecomEvents(): ", "recomEvents = [", recomEvents, "]")
        /*@formatter:on*/

        val rowIds: List<String> = recomEvents
            .mapNotNull { it.recomEvents }
            .flatten()
            .mapNotNull { it.rowId }

        for (rowId: String in rowIds) {
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
        /*@formatter:off*/ Logger.i(TAG, "deleteRecomEventsByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/

        val count = database.delete(
            table = RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT,
            whereClause = "${RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED} < '$outdatedTime'"
        )
        database.cleanUnlinkedRecomVariantIds()
        return count
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerRecomEventsImpl::class.java.simpleName
    }
}