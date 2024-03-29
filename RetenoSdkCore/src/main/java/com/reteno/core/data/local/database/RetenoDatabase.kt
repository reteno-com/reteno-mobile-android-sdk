package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException

internal interface RetenoDatabase {

    fun query(
        table: String, columns: Array<String>, selection: String? = null,
        selectionArgs: Array<String>? = null, groupBy: String? = null, having: String? = null,
        orderBy: String? = null, limit: String? = null
    ): Cursor

    fun rawQuery(rawQuery: String, selectionArgs: Array<out String>? = null): Cursor

    fun insert(table: String, nullColumnHack: String? = null, contentValues: ContentValues): Long

    fun insertMultiple(
        table: String,
        nullColumnHack: String? = null,
        contentValues: List<ContentValues>
    ): List<Long>

    @Throws(SQLException::class)
    fun insertOrThrow(table: String, nullColumnHack: String? = null, contentValues: ContentValues): Long

    fun update(
        table: String,
        contentValues: ContentValues,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int

    fun delete(table: String, whereClause: String? = null, whereArgs: Array<String?>? = null): Int

    fun getRowCount(tableName: String, whereClause: String? = null, whereArgs: Array<String?>? = null): Long

    /**
     * Call this method each time you remove any record from Event table (Child table)
     */
    fun cleanUnlinkedEvents()

    /**
     * Call this method each time you remove any record from RecomEvent table (Child table)
     */
    fun cleanUnlinkedRecomVariantIds()
}