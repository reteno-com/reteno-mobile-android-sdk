package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.database.util.getInAppMessage
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerInAppMessagesImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerInAppMessages {

    override fun insertInAppMessages(inApps: List<InAppMessageDb>) {
        /*@formatter:off*/ Logger.i(TAG, "insertInAppMessages(): ", "inApps = [", inApps, "]")
        /*@formatter:on*/

        val contentValues = inApps.toContentValuesList()
        database.insertMultiple(
            table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
            contentValues = contentValues
        )
    }

    override fun getInAppMessages(limit: Int?): List<InAppMessageDb> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val inAppMessages: MutableList<InAppMessageDb> = mutableListOf()

        var cursor: Cursor? = null
        try {
            cursor = database.query(
                table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
                columns = InAppMessageSchema.getAllColumns(),
                orderBy = "${DbSchema.COLUMN_TIMESTAMP} ASC",
                limit = limit?.toString()
            )
            while (cursor.moveToNext()) {
                val timestamp = cursor.getStringOrNull(cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
                val inApp = cursor.getInAppMessage()

                if (inApp != null) {
                    inAppMessages.add(inApp)
                } else {
                    val rowId = cursor.getLongOrNull(cursor.getColumnIndex(InAppMessageSchema.COLUMN_IAM_ROW_ID))
                    val exception =
                        SQLException("Unable to read data from SQL database. timeStamp=$timestamp, inApp=$inApp")
                    if (rowId == null) {
                        /*@formatter:off*/ Logger.e(TAG, "getInAppMessages(). rowId is NULL ", exception)
                        /*@formatter:on*/
                    } else {
                        database.delete(
                            table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
                            whereClause = "${InAppMessageSchema.COLUMN_IAM_ROW_ID}=?",
                            whereArgs = arrayOf(rowId.toString())
                        )
                        /*@formatter:off*/ Logger.e(TAG, "getInAppMessages(). Removed invalid entry from database. inApp=$inApp ", exception)
                        /*@formatter:on*/
                    }
                }
            }
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): Unable to get InAppMessage from the table.", t)
            /*@formatter:on*/
        } finally {
            cursor?.close()
        }
        return inAppMessages
    }

    override fun getInAppMessagesCount(): Long {
        return database.getRowCount(InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE)
    }

    override fun deleteInAppMessage(inApp: InAppMessageDb): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "deleteInAppMessage(): ", "inApp = [", inApp, "]")
        /*@formatter:on*/

        val removedRecordsCount = database.delete(
            table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
            whereClause = "${InAppMessageSchema.COLUMN_IAM_ROW_ID}=?",
            whereArgs = arrayOf(inApp.rowId)
        )

        return removedRecordsCount > 0
    }

    override fun deleteAllInAppMessages() {
        /*@formatter:off*/ Logger.i(TAG, "deleteAllInAppMessages(): ", "")
        /*@formatter:on*/

        database.delete(table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE)
    }

    companion object {
        private val TAG: String = RetenoDatabaseManagerInAppMessagesImpl::class.java.simpleName
    }
}