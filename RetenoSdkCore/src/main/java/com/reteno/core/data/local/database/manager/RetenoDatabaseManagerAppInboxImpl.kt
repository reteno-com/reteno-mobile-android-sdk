package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.util.getAppInbox
import com.reteno.core.data.local.database.util.putAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb

internal class RetenoDatabaseManagerAppInboxImpl(private val database: RetenoDatabase) : RetenoDatabaseManagerAppInbox {

    override fun insertAppInboxMessage(message: AppInboxMessageDb) {
        database.insert(
            table = AppInboxSchema.TABLE_NAME_APP_INBOX,
            contentValues = ContentValues().apply { putAppInbox(message) }
        )
    }

    override fun getAppInboxMessages(limit: Int?): List<AppInboxMessageDb> = database.readRows(
        table = AppInboxSchema.TABLE_NAME_APP_INBOX,
        rowIdColumn = AppInboxSchema.COLUMN_APP_INBOX_ID,
        query = {
            query(
                table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                columns = AppInboxSchema.getAllColumns(),
                orderBy = "${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC",
                limit = limit?.toString()
            )
        },
        read = { getAppInbox() }
    )

    override fun getAppInboxMessagesCount(): Long = database.getRowCount(AppInboxSchema.TABLE_NAME_APP_INBOX)

    override fun deleteAppInboxMessages(appInboxMessages: List<AppInboxMessageDb>) {
        appInboxMessages.forEach {
            database.delete(
                table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                whereClause = "${AppInboxSchema.COLUMN_APP_INBOX_ID}=?",
                whereArgs = arrayOf(it.id)
            )
        }
    }

    override fun deleteAllAppInboxMessages() {
        database.delete(table = AppInboxSchema.TABLE_NAME_APP_INBOX)
    }

    override fun deleteAppInboxMessagesByTime(outdatedTime: String): Int = database.delete(
        table = AppInboxSchema.TABLE_NAME_APP_INBOX,
        whereClause = "${AppInboxSchema.COLUMN_APP_INBOX_TIME} < ?",
        whereArgs = arrayOf(outdatedTime)
    )
}
