package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.LogEventSchema
import com.reteno.core.data.local.database.util.getLogEvent
import com.reteno.core.data.local.database.util.putLogEvent
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb

internal class RetenoDatabaseManagerLogEventImpl(private val database: RetenoDatabase) : RetenoDatabaseManagerLogEvent {

    override fun insertLogEvent(logEvent: RetenoLogEventDb) {
        database.insert(
            table = LogEventSchema.TABLE_NAME_LOG_EVENT,
            contentValues = ContentValues().apply { putLogEvent(logEvent) }
        )
    }

    override fun getLogEvents(limit: Int?): List<RetenoLogEventDb> = database.readRows(
        table = LogEventSchema.TABLE_NAME_LOG_EVENT,
        rowIdColumn = LogEventSchema.COLUMN_LOG_EVENT_ROW_ID,
        query = {
            query(
                table = LogEventSchema.TABLE_NAME_LOG_EVENT,
                columns = LogEventSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
        },
        read = { getLogEvent() }
    )

    override fun getLogEventsCount(): Long = database.getRowCount(LogEventSchema.TABLE_NAME_LOG_EVENT)

    override fun deleteLogEvents(logEvents: List<RetenoLogEventDb>): Boolean {
        var removedRecordsCount = 0
        logEvents.mapNotNull { it.rowId }.forEach { rowId ->
            removedRecordsCount = database.delete(
                table = LogEventSchema.TABLE_NAME_LOG_EVENT,
                whereClause = "${LogEventSchema.COLUMN_LOG_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
        return removedRecordsCount > 0
    }
}
