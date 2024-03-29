package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.util.getAppInbox
import com.reteno.core.data.local.database.util.putAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerAppInboxImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerAppInbox {

    private val contentValues = ContentValues()

    override fun insertAppInboxMessage(message: AppInboxMessageDb) {
        contentValues.putAppInbox(message)
        database.insert(table = AppInboxSchema.TABLE_NAME_APP_INBOX, contentValues = contentValues)
        contentValues.clear()
    }

    override fun getAppInboxMessages(limit: Int?): List<AppInboxMessageDb> {
        /*@formatter:off*/ Logger.i(TAG, "getAppInboxMessages(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val inboxMessage: MutableList<AppInboxMessageDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                columns = AppInboxSchema.getAllColumns(),
                orderBy = "${AppInboxSchema.COLUMN_APP_INBOX_TIME} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_TIME))
                val inbox = cursor.getAppInbox()

                if (inbox != null) {
                    inboxMessage.add(inbox)
                } else {
                    val rowId =
                        cursor.getStringOrNull(cursor.getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, inboxMessage=null. rowId = $rowId")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getAppInboxMessages(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                            whereClause = "${AppInboxSchema.COLUMN_APP_INBOX_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getAppInboxMessages(). Removed invalid entry from database. inboxMessage=null, rowId = $rowId", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get AppInboxMessage from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return inboxMessage
    }

    override fun getAppInboxMessagesCount(): Long = database.getRowCount(AppInboxSchema.TABLE_NAME_APP_INBOX)

    override fun deleteAppInboxMessages(appInboxMessages: List<AppInboxMessageDb>) {
        /*@formatter:off*/ Logger.i(TAG, "deleteAppInboxMessages(): ", "appInboxMessages = [", appInboxMessages, "]")
        /*@formatter:on*/

        val rowIds: List<String> = appInboxMessages
            .map { it.id }

        for (rowId: String in rowIds) {
            database.delete(
                table = AppInboxSchema.TABLE_NAME_APP_INBOX,
                whereClause = "${AppInboxSchema.COLUMN_APP_INBOX_ID}=?",
                whereArgs = arrayOf(rowId)
            )
        }
    }

    override fun deleteAllAppInboxMessages() {
        /*@formatter:off*/ Logger.i(TAG, "deleteAllAppInboxMessages(): ", "")
        /*@formatter:on*/

        database.delete(table = AppInboxSchema.TABLE_NAME_APP_INBOX)
    }

    override fun deleteAppInboxMessagesByTime(outdatedTime: String): Int {
        /*@formatter:off*/ Logger.i(TAG, "deleteAppInboxMessagesByTime(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/

        return database.delete(
            table = AppInboxSchema.TABLE_NAME_APP_INBOX,
            whereClause = "${AppInboxSchema.COLUMN_APP_INBOX_TIME} < '$outdatedTime'"
        )
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerAppInboxImpl::class.java.simpleName
    }
}