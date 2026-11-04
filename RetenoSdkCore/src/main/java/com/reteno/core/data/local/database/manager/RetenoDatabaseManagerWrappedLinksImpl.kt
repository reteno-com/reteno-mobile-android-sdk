package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.WrappedLinkSchema

internal class RetenoDatabaseManagerWrappedLinksImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerWrappedLink {

    override fun insertWrappedLink(url: String) {
        database.insert(
            table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
            contentValues = ContentValues().apply { put(WrappedLinkSchema.COLUMN_URL, url) }
        )
    }

    override fun getWrappedLinks(limit: Int?): List<String> = database.readRows(
        table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
        rowIdColumn = WrappedLinkSchema.COLUMN_ROW_ID,
        query = {
            query(
                table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
                columns = WrappedLinkSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
        },
        read = { getStringOrNull(getColumnIndexOrThrow(WrappedLinkSchema.COLUMN_URL)) }
    )

    override fun getWrappedLinksCount(): Long = database.getRowCount(WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK)

    override fun deleteWrappedLinks(count: Int, oldest: Boolean) {
        val order = if (oldest) "ASC" else "DESC"
        database.delete(
            table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
            whereClause = "${WrappedLinkSchema.COLUMN_ROW_ID} in (select ${WrappedLinkSchema.COLUMN_ROW_ID} " +
                "from ${WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK} ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"
        )
    }

    override fun deleteWrappedLinksByTime(outdatedTime: String): Int = database.delete(
        table = WrappedLinkSchema.TABLE_NAME_WRAPPED_LINK,
        whereClause = "${DbSchema.COLUMN_TIMESTAMP} < datetime(?)",
        whereArgs = arrayOf(outdatedTime)
    )
}
