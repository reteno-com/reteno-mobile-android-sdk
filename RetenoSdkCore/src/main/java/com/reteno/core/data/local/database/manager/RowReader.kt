package com.reteno.core.data.local.database.manager

import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.util.Logger

internal fun <T : Any> RetenoDatabase.readRows(
    table: String,
    rowIdColumn: String,
    query: RetenoDatabase.() -> Cursor,
    read: Cursor.() -> T?
): List<T> {
    val rows = mutableListOf<T>()
    val unreadableRowIds = mutableListOf<String?>()
    try {
        query().use { cursor ->
            while (cursor.moveToNext()) {
                val row = cursor.readOrNull(read)
                if (row == null) {
                    unreadableRowIds += cursor.getStringOrNull(cursor.getColumnIndexOrThrow(rowIdColumn))
                } else {
                    rows += row
                }
            }
        }
    } catch (e: Exception) {
        Logger.e(TAG, "readRows(): unable to read $table", e)
    }
    unreadableRowIds.forEach { removeUnreadableRow(table, rowIdColumn, it) }
    return rows
}

private fun <T : Any> Cursor.readOrNull(read: Cursor.() -> T?): T? =
    try {
        read()
    } catch (e: Exception) {
        null
    }

private fun RetenoDatabase.removeUnreadableRow(table: String, rowIdColumn: String, rowId: String?) {
    val error = SQLException("Unable to read data from SQL database")
    if (rowId == null) {
        Logger.e(TAG, "readRows(): unreadable row without id in $table", error)
        return
    }
    delete(table = table, whereClause = "$rowIdColumn=?", whereArgs = arrayOf(rowId))
    Logger.e(TAG, "readRows(): removed unreadable row from $table", error)
}

private const val TAG = "RowReader"
