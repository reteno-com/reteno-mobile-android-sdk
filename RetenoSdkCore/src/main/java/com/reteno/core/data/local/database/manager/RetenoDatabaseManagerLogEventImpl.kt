package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.LogEventSchema
import com.reteno.core.data.local.database.util.getLogEvent
import com.reteno.core.data.local.database.util.putLogEvent
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerLogEventImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerLogEvent {

    private val contentValues = ContentValues()

    override fun insertLogEvent(logEvent: RetenoLogEventDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertLogEvent(): ", "logEvent = [", logEvent, "]")
        /*@formatter:on*/

        contentValues.putLogEvent(logEvent)
        database.insert(table = LogEventSchema.TABLE_NAME_LOG_EVENT, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getLogEvents(limit: Int?): List<RetenoLogEventDb> {
        /*@formatter:off*/ Logger.i(TAG, "getLogEvents(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val logEvents: MutableList<RetenoLogEventDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = LogEventSchema.TABLE_NAME_LOG_EVENT,
                columns = LogEventSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val logEvent = cursor.getLogEvent()

                if (logEvent != null) {
                    logEvents.add(logEvent)
                } else {
                    val rowId = cursor.getLongOrNull(cursor.getColumnIndex(LogEventSchema.COLUMN_LOG_EVENT_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, logEvent=$logEvent")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getLogEvents(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = LogEventSchema.TABLE_NAME_LOG_EVENT,
                            whereClause = "${LogEventSchema.COLUMN_LOG_EVENT_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getLogEvents(). Removed invalid entry from database. logEvent=$logEvent ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get LogEvent from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return logEvents
    }

    override fun getLogEventsCount(): Long = database.getRowCount(LogEventSchema.TABLE_NAME_LOG_EVENT)

    override fun deleteLogEvents(logEvents: List<RetenoLogEventDb>): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteLogEvent(): ", "logEvent = [", logEvents, "]")
        /*@formatter:on*/

        val rowIds: List<String> = logEvents.mapNotNull { it.rowId }

        var removedRecordsCount = 0

        for (rowId: String in rowIds) {
            removedRecordsCount = database.delete(
                table = LogEventSchema.TABLE_NAME_LOG_EVENT,
                whereClause = "${LogEventSchema.COLUMN_LOG_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }

        return removedRecordsCount > 0
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerLogEventImpl::class.java.simpleName
    }
}