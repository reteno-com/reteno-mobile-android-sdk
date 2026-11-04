package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.database.util.getEvent
import com.reteno.core.data.local.database.util.putEvents
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb

internal class RetenoDatabaseManagerEventsImpl(private val database: RetenoDatabase) : RetenoDatabaseManagerEvents {

    override fun insertEvents(events: EventsDb) {
        val parentRowId = findParentRowId(events) ?: database.insert(
            table = EventsSchema.TABLE_NAME_EVENTS,
            contentValues = ContentValues().apply { putEvents(events) }
        )
        database.insertMultiple(
            table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
            contentValues = events.eventList.toContentValuesList(parentRowId)
        )
    }

    private fun findParentRowId(events: EventsDb): Long? {
        val externalUserId = events.externalUserId
        val (selection, selectionArgs) = if (externalUserId == null) {
            "${EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID} IS NULL" to
                arrayOf(events.deviceId)
        } else {
            "${EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID}=?" to
                arrayOf(events.deviceId, externalUserId)
        }
        return readParents {
            query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns(),
                selection = selection,
                selectionArgs = selectionArgs
            )
        }.firstOrNull()?.first
    }

    override fun getEvents(limit: Int?): List<EventsDb> {
        val parents = readParents {
            query(table = EventsSchema.TABLE_NAME_EVENTS, columns = EventsSchema.getAllColumns())
        }
        val events = parents.mapNotNull { (parentRowId, parent) ->
            val eventList = readEvents {
                query(
                    table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                    columns = EventsSchema.EventSchema.getAllColumns(),
                    selection = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                    selectionArgs = arrayOf(parentRowId.toString()),
                    limit = limit?.toString()
                )
            }
            if (eventList.isEmpty()) null else parent.copy(eventList = eventList)
        }
        database.cleanUnlinkedEvents()
        return events
    }

    override fun getEventsCount(): Long = database.getRowCount(EventsSchema.EventSchema.TABLE_NAME_EVENT)

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedEvents] each time you remove events from Event table (Child table)
     */
    override fun deleteEvents(events: EventsDb) {
        events.eventList.mapNotNull { it.rowId }.forEach { rowId ->
            database.delete(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                whereClause = "${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
        database.cleanUnlinkedEvents()
    }

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedEvents] each time you remove events from Event table (Child table)
     */
    override fun deleteEventsByTime(outdatedTime: String): List<EventDb> {
        val outdated = "${EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED} < ?"
        val events = readEvents {
            query(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                columns = EventsSchema.EventSchema.getAllColumns(),
                selection = outdated,
                selectionArgs = arrayOf(outdatedTime)
            )
        }
        database.delete(
            table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
            whereClause = outdated,
            whereArgs = arrayOf(outdatedTime)
        )
        database.cleanUnlinkedEvents()
        return events
    }

    private fun readParents(query: RetenoDatabase.() -> Cursor): List<Pair<Long, EventsDb>> = database.readRows(
        table = EventsSchema.TABLE_NAME_EVENTS,
        rowIdColumn = EventsSchema.COLUMN_EVENTS_ID,
        query = query,
        read = { readParent() }
    )

    private fun Cursor.readParent(): Pair<Long, EventsDb>? {
        val deviceId = getStringOrNull(getColumnIndexOrThrow(EventsSchema.COLUMN_EVENTS_DEVICE_ID)) ?: return null
        val externalUserId = getStringOrNull(getColumnIndexOrThrow(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID))
        val parentRowId = getLong(getColumnIndexOrThrow(EventsSchema.COLUMN_EVENTS_ID))
        return parentRowId to EventsDb(deviceId, externalUserId, emptyList())
    }

    private fun readEvents(query: RetenoDatabase.() -> Cursor): List<EventDb> = database.readRows(
        table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
        rowIdColumn = EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID,
        query = query,
        read = { getEvent() }
    )
}
