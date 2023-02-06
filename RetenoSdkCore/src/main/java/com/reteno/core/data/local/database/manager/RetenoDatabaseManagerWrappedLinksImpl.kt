package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.WrappedLinkSchema
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.SQLException

internal class RetenoDatabaseManagerWrappedLinksImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerWrappedLink {

    private val contentValues = ContentValues()

    override fun insertWrappedLink(url: String) {
        /*@formatter:off*/ Logger.i(TAG, "insertWrappedLink(): ", "url = [", url, "]")
        /*@formatter:on*/
        contentValues.put(WrappedLinkSchema.COLUMN_URL, url)
        database.insert(table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getWrappedLinks(limit: Int?): List<String> {
        /*@formatter:off*/ Logger.i(TAG, "getWrappedLinks(): ", "limit = [", limit, "]")
        /*@formatter:on*/
        val wrappedLinks: MutableList<String> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
                columns = WrappedLinkSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp: String? =
                    cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val wrappedLink: String? =
                    cursor.getStringOrNull(cursor.getColumnIndex(WrappedLinkSchema.COLUMN_URL))

                if (wrappedLink != null) {
                    wrappedLinks.add(wrappedLink)
                } else {
                    val rowId: Long? =
                        cursor.getLongOrNull(cursor.getColumnIndex(WrappedLinkSchema.COLUMN_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, wrappedLinkUrl=$wrappedLink")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getWrappedLinks(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
                            whereClause = "${WrappedLinkSchema.COLUMN_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getWrappedLinks(). Removed invalid entry from database. url=$wrappedLink ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get WrappedLinks from the table", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return wrappedLinks
    }

    override fun getWrappedLinksCount(): Long = database.getRowCount(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK)

    override fun deleteWrappedLinks(count: Int, oldest: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "deleteWrappedLinks(): ", "count = [", count, "], oldest = [", oldest, "]")
        /*@formatter:on*/
        val order = if (oldest) "ASC" else "DESC"
        database.delete(
            table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
            whereClause = "${WrappedLinkSchema.COLUMN_ROW_ID} in (select ${WrappedLinkSchema.COLUMN_ROW_ID} from ${WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK} ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order LIMIT $count)"
        )
    }

    override fun deleteWrappedLinksByTime(outdatedTime: String): Int {
        /*@formatter:off*/ Logger.i(TAG, "deleteWrappedLinksByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/
        return database.delete(
            table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
            whereClause = "${DbSchema.COLUMN_TIMESTAMP} < '$outdatedTime'"
        )
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerWrappedLinksImpl::class.java.simpleName
    }
}