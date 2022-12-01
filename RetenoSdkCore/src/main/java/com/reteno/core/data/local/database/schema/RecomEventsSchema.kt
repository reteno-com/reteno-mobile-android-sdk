package com.reteno.core.data.local.database.schema

internal object RecomEventsSchema {
    internal const val TABLE_NAME_RECOM_EVENTS = "RecomEvents"

    internal const val COLUMN_RECOM_VARIANT_ID = "recomVariantId"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_RECOM_EVENTS" +
                "($COLUMN_RECOM_VARIANT_ID TEXT PRIMARY KEY NOT NULL)"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_RECOM_VARIANT_ID
    )

    internal object RecomEventSchema {
        internal const val TABLE_NAME_RECOM_EVENT = "RecomEvent"

        internal const val COLUMN_RECOM_EVENT_ROW_ID = "rowId"
        internal const val COLUMN_RECOM_EVENT_PRODUCT_ID = "productId"
        internal const val COLUMN_RECOM_EVENT_OCCURRED = "occurred"
        internal const val COLUMN_RECOM_EVENT_TYPE = "eventType"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_RECOM_EVENT" +
                    "(" +
                    "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} TEXT NOT NULL, " +
                    "$COLUMN_RECOM_EVENT_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_RECOM_EVENT_PRODUCT_ID TEXT, " +
                    "$COLUMN_RECOM_EVENT_OCCURRED TIMESTAMP, " +
                    "$COLUMN_RECOM_EVENT_TYPE TEXT, " +
                    "FOREIGN KEY (${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}) REFERENCES ${RecomEventsSchema.TABLE_NAME_RECOM_EVENTS} (${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID})" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            RecomEventsSchema.COLUMN_RECOM_VARIANT_ID,
            COLUMN_RECOM_EVENT_ROW_ID,
            COLUMN_RECOM_EVENT_PRODUCT_ID,
            COLUMN_RECOM_EVENT_OCCURRED,
            COLUMN_RECOM_EVENT_TYPE
        )
    }
}