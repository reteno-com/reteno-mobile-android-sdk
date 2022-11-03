package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException

interface RetenoDatabase {
    fun query(
        table: String, columns: Array<String>, selection: String?,
        selectionArgs: Array<String>?, groupBy: String?, having: String?,
        orderBy: String?
    ): Cursor

    fun query(
        table: String, columns: Array<String>, selection: String?,
        selectionArgs: Array<String>?, groupBy: String?, having: String?,
        orderBy: String?, limit: String?
    ): Cursor

    fun rawQuery(rawQuery: String, selectionArgs: Array<out String>?): Cursor

    // TODO This is a very dangerous method. Should be hidden behind internal at least!
    fun execSql(rawQuery: String)

    fun insert(table: String, nullColumnHack: String?, contentValues: ContentValues): Long

    fun insertMultiple(
        table: String,
        nullColumnHack: String?,
        contentValues: List<ContentValues>
    ): List<Long>

    @Throws(SQLException::class)
    fun insertOrThrow(table: String, nullColumnHack: String?, contentValues: ContentValues): Long

    fun update(
        table: String,
        contentValues: ContentValues,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int

    fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?)

    fun getRowCount(tableName: String): Long
}