package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.database.util.getEvent
import com.reteno.core.data.local.database.util.putEvents
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.util.Logger
import com.reteno.core.util.allElementsNotNull

internal class RetenoDatabaseManagerEventsImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerEvents {

    private val contentValues = ContentValues()

    override fun insertEvents(events: EventsDb) {
        /*@formatter:off*/ Logger.i(TAG, "insertEvents(): ", "INSERT")
        /*@formatter:on*/
        var parentRowId: Long = -1L

        var cursor: Cursor? = null
        try {
            cursor = if (events.externalUserId == null) {
                database.query(
                    table = EventsSchema.TABLE_NAME_EVENTS,
                    columns = EventsSchema.getAllColumns(),
                    selection = "${EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID} IS NULL",
                    selectionArgs = arrayOf(events.deviceId)
                )
            } else {
                database.query(
                    table = EventsSchema.TABLE_NAME_EVENTS,
                    columns = EventsSchema.getAllColumns(),
                    selection = "${EventsSchema.COLUMN_EVENTS_DEVICE_ID}=? AND ${EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID}=?",
                    selectionArgs = arrayOf(events.deviceId, events.externalUserId)
                )
            }

            if (cursor.moveToFirst()) {
                cursor.getLongOrNull(cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_ID))?.let {
                    parentRowId = it
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Events from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        if (parentRowId == -1L) {
            contentValues.putEvents(events)
            parentRowId = database.insert(table = EventsSchema.TABLE_NAME_EVENTS, contentValues = contentValues)
            contentValues.clear()
        }

        val eventListContentValues = events.eventList.toContentValuesList(parentRowId)
        database.insertMultiple(table = EventsSchema.EventSchema.TABLE_NAME_EVENT, contentValues = eventListContentValues)
    }

    override fun getEvents(limit: Int?): List<EventsDb> {
        /*@formatter:off*/ Logger.i(TAG, "getEvents(): ", "limit = [", limit, "]")
        /*@formatter:on*/
        var cursor: Cursor? = null

        val eventsParentTableList: MutableMap<String, EventsDb> = mutableMapOf()
        try {
            cursor = database.query(
                table = EventsSchema.TABLE_NAME_EVENTS,
                columns = EventsSchema.getAllColumns()
            )
            while (cursor.moveToNext()) {
                val eventsId = cursor.getLongOrNull(cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_ID))
                val deviceId = cursor.getStringOrNull(cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_DEVICE_ID))
                val externalUserId = cursor.getStringOrNull(cursor.getColumnIndex(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID))

                if (allElementsNotNull(eventsId, deviceId)) {
                    eventsParentTableList[eventsId!!.toString()] =
                        EventsDb(deviceId!!, externalUserId, listOf())
                } else {
                    val exception =
                        SQLException("Unable to read data from SQL database getEvents(). eventsId=$eventsId, deviceId=$deviceId, externalUserId=$externalUserId")
                    if (eventsId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getEvents(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = EventsSchema.TABLE_NAME_EVENTS,
                            whereClause = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                            whereArgs = arrayOf(eventsId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getEvents(). Removed invalid entry from database. eventsId=$eventsId, deviceId=$deviceId, externalUserId=$externalUserId", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Events from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        val eventsResult: MutableList<EventsDb> = mutableListOf()
        for (eventsParent in eventsParentTableList.entries.iterator()) {
            val foreignKeyRowId = eventsParent.key
            val eventList: MutableList<EventDb> = mutableListOf()

            var cursorChild: Cursor? = null
            try {
                cursorChild = database.query(
                    table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                    columns = EventsSchema.EventSchema.getAllColumns(),
                    selection = "${EventsSchema.COLUMN_EVENTS_ID}=?",
                    selectionArgs = arrayOf(foreignKeyRowId),
                    limit = limit?.toString()
                )

                while (cursorChild.moveToNext()) {
                    val event = cursorChild.getEvent()
                    if (event != null) {
                        eventList.add(event)
                    } else {
                        val rowId = cursorChild.getLongOrNull(
                            cursorChild.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID)
                        )
                        val exception =
                            SQLException("Unable to read data from SQL database. event=$event")
                        if (rowId == null) {
                            /*@formatter:off*/ Logger.e(TAG, "getEvents(). rowId is NULL ", exception)
                            /*@formatter:on*/
                        } else {
                            database.delete(
                                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                                whereClause = "${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?",
                                whereArgs = arrayOf(rowId.toString())
                            )
                            /*@formatter:off*/ Logger.e(TAG, "getEvents(). Removed invalid entry from database. event=$event ", exception)
                            /*@formatter:on*/
                        }
                    }
                }
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get Events from the table.", t)
                /*@formatter:on*/
            } finally {
                cursorChild?.close()
            }


            if (eventList.isNotEmpty()) {
                val singleEventResult = EventsDb(
                    eventsParent.value.deviceId,
                    eventsParent.value.externalUserId,
                    eventList
                )
                eventsResult.add(singleEventResult)
            }
        }

        database.cleanUnlinkedEvents()

        return eventsResult
    }

    override fun getEventsCount(): Long = database.getRowCount(EventsSchema.EventSchema.TABLE_NAME_EVENT)

    /**
     * Call [com.reteno.core.data.local.database.RetenoDatabase.cleanUnlinkedEvents] each time you remove events from Event table (Child table)
     */
    override fun deleteEvents(events: EventsDb) {
        /*@formatter:off*/ Logger.i(TAG, "deleteEvents(): ", "events = [", events, "]")
        /*@formatter:on*/

        val rowIds: List<String> = events.eventList
            .mapNotNull { it.rowId }

        for (rowId: String in rowIds) {
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
        /*@formatter:off*/ Logger.i(TAG, "deleteEventsByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/
        val whereClause = "${EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED} < '$outdatedTime'"

        var cursor: Cursor? = null
        val eventList = mutableListOf<EventDb>()
        try {
            cursor = database.query(
                table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                columns = EventsSchema.EventSchema.getAllColumns(),
                selection = whereClause
            )

            while (cursor.moveToNext()) {
                val event = cursor.getEvent()
                if (event != null) {
                    eventList.add(event)
                } else {
                    val rowId = cursor.getLongOrNull(
                        cursor.getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID)
                    )
                    val exception =
                        SQLException("deleteEventsByTime() Unable to read data from SQL database. event=$event")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "deleteEventsByTime(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
                            whereClause = "${EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "deleteEventsByTime(). Removed invalid entry from database. event=$event ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "deleteEventsByTime() handleSQLiteError(): Unable to get Events from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }

        database.delete(
            table = EventsSchema.EventSchema.TABLE_NAME_EVENT,
            whereClause = whereClause
        )
        database.cleanUnlinkedEvents()

        return eventList
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerEventsImpl::class.java.simpleName
    }
}