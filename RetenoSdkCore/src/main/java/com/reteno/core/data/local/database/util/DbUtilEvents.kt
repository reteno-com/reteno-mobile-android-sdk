package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.mapper.listFromJson

fun ContentValues.putEvents(events: EventsDb) {
    put(EventsSchema.COLUMN_EVENTS_DEVICE_ID, events.deviceId)
    put(EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID, events.externalUserId)
}

fun List<EventDb>.toContentValuesList(parentRowId: Long): List<ContentValues> {
    val contentValues = mutableListOf<ContentValues>()

    for (event in this) {
        val singleContentValues = ContentValues().apply {
            put(EventsSchema.COLUMN_EVENTS_ID, parentRowId)
            put(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY, event.eventTypeKey)
            put(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED, event.occurred)
            putJsonIfPresent(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS, event.params)
        }
        contentValues.add(singleContentValues)
    }

    return contentValues
}

fun Cursor.getEvent(): EventDb? {
    val rowId = getString(getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_ROW_ID))
    val eventTypeKey = getStringOrNull(getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY))
    val occurred = getStringOrNull(getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_OCCURRED))

    val paramsString = getStringOrNull(getColumnIndex(EventsSchema.EventSchema.COLUMN_EVENT_PARAMS))
    val params: List<ParameterDb>? = paramsString?.listFromJson()

    if (eventTypeKey == null || occurred == null) return null
    return EventDb(rowId = rowId, eventTypeKey = eventTypeKey, occurred = occurred, params = params)
}
