package com.reteno.core.data.local.database.schema

internal object EventsSchema {
    internal const val TABLE_NAME_EVENTS = "Events"

    internal const val COLUMN_EVENTS_ID = "events_id"

    internal const val COLUMN_EVENTS_DEVICE_ID = "deviceId"
    internal const val COLUMN_EVENTS_EXTERNAL_USER_ID = "externalUserId"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_EVENTS" +
                "(" +
                "$COLUMN_EVENTS_ID INTEGER PRIMARY KEY, " +
                "$COLUMN_EVENTS_DEVICE_ID TEXT, " +
                "$COLUMN_EVENTS_EXTERNAL_USER_ID TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_EVENTS_ID,
        COLUMN_EVENTS_DEVICE_ID,
        COLUMN_EVENTS_EXTERNAL_USER_ID
    )

    internal object EventSchema {
        internal const val TABLE_NAME_EVENT = "Event"

        internal const val COLUMN_EVENT_ROW_ID = "row_id"

        internal const val COLUMN_EVENT_TYPE_KEY = "eventTypeKey"
        internal const val COLUMN_EVENT_OCCURRED = "occurred"
        internal const val COLUMN_EVENT_PARAMS = "params"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_EVENT" +
                    "(" +
                    "${EventsSchema.COLUMN_EVENTS_ID} INTEGER NOT NULL, " +
                    "$COLUMN_EVENT_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_EVENT_TYPE_KEY TEXT, " +
                    "$COLUMN_EVENT_OCCURRED TIMESTAMP, " +
                    "$COLUMN_EVENT_PARAMS TEXT, " +
                    "FOREIGN KEY (${EventsSchema.COLUMN_EVENTS_ID}) REFERENCES ${EventsSchema.TABLE_NAME_EVENTS} (${EventsSchema.COLUMN_EVENTS_ID})" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            EventsSchema.COLUMN_EVENTS_ID,
            COLUMN_EVENT_ROW_ID,
            COLUMN_EVENT_TYPE_KEY,
            COLUMN_EVENT_OCCURRED,
            COLUMN_EVENT_PARAMS
        )
    }
}