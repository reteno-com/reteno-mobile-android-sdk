package com.reteno.core.data.local.database.schema

internal object WrappedLinkSchema {
    internal const val TABLE_NAME_WRAPPED_LINK = "WrappedLink"

    internal const val COLUMN_ROW_ID = "row_id"
    internal const val COLUMN_URL = "url"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_WRAPPED_LINK" +
                "(" +
                "$COLUMN_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_URL TEXT" +
                ")"

    fun getAllColumns(): Array<String> = arrayOf(
        COLUMN_ROW_ID,
        DbSchema.COLUMN_TIMESTAMP,
        COLUMN_URL
    )
}