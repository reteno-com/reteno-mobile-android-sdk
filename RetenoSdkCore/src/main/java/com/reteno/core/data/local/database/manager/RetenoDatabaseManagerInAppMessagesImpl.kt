package com.reteno.core.data.local.database.manager

import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.database.util.getInAppMessage
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.database.util.toSegmentContentValuesList
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.util.Logger

internal class RetenoDatabaseManagerInAppMessagesImpl(private val database: RetenoDatabase) :
    RetenoDatabaseManagerInAppMessages {

    override fun insertInAppMessages(inApps: List<InAppMessageDb>) {
        /*@formatter:off*/ Logger.i(TAG, "insertInAppMessages(): ", "inApps = [", inApps, "]")
        /*@formatter:on*/

        val contentValues = inApps.toContentValuesList()
        val rowIds = database.insertMultiple(
            table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
            contentValues = contentValues
        )

        if (rowIds.size == inApps.size) {
            val rowIdToSegmentList = rowIds.zip(inApps)
                .filter { (_, inApp) -> inApp.segment != null }
                .map { it.first to it.second.segment!! }

            val segmentContentValues = rowIdToSegmentList.toSegmentContentValuesList()
            database.insertMultiple(
                table = InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT,
                contentValues = segmentContentValues
            )
        }
    }

    override fun getInAppMessages(limit: Int?): List<InAppMessageDb> {
        /*@formatter:off*/ Logger.i(TAG, "getInAppMessages(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val inAppMessages: MutableList<InAppMessageDb> = mutableListOf()
        val rawQueryLimit: String = limit?.let { " LIMIT $it" } ?: ""

        var cursor: Cursor? = null
        try {
            val rawQuery = "SELECT" +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_ROW_ID} AS ${InAppMessageSchema.COLUMN_IAM_ROW_ID}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_ID} AS ${InAppMessageSchema.COLUMN_IAM_ID}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_INSTANCE_ID} AS ${InAppMessageSchema.COLUMN_IAM_INSTANCE_ID}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${DbSchema.COLUMN_TIMESTAMP} AS ${DbSchema.COLUMN_TIMESTAMP}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_DISPLAY_RULES} AS ${InAppMessageSchema.COLUMN_IAM_DISPLAY_RULES}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_LAST_SHOW_TIME} AS ${InAppMessageSchema.COLUMN_IAM_LAST_SHOW_TIME}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_SHOW_COUNT} AS ${InAppMessageSchema.COLUMN_IAM_SHOW_COUNT}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_LAYOUT_TYPE} AS ${InAppMessageSchema.COLUMN_IAM_LAYOUT_TYPE}," +
                    "  ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_MODEL} AS ${InAppMessageSchema.COLUMN_IAM_MODEL}," +
                    "  ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.SegmentSchema.COLUMN_SEGMENT_ID} AS ${InAppMessageSchema.SegmentSchema.COLUMN_SEGMENT_ID}," +
                    "  ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.SegmentSchema.COLUMN_IS_IN_SEGMENT} AS ${InAppMessageSchema.SegmentSchema.COLUMN_IS_IN_SEGMENT}," +
                    "  ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.SegmentSchema.COLUMN_LAST_CHECK_TIME} AS ${InAppMessageSchema.SegmentSchema.COLUMN_LAST_CHECK_TIME}," +
                    "  ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.SegmentSchema.COLUMN_CHECK_STATUS_CODE} AS ${InAppMessageSchema.SegmentSchema.COLUMN_CHECK_STATUS_CODE}," +
                    "  ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.SegmentSchema.COLUMN_RETRY_AFTER} AS ${InAppMessageSchema.SegmentSchema.COLUMN_RETRY_AFTER}" +
                    " FROM ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}" +
                    "  LEFT JOIN ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT} ON ${InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE}.${InAppMessageSchema.COLUMN_IAM_ROW_ID} = ${InAppMessageSchema.SegmentSchema.TABLE_NAME_SEGMENT}.${InAppMessageSchema.COLUMN_IAM_ROW_ID}" +
                    rawQueryLimit
            cursor = database.rawQuery(rawQuery)
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

    override fun deleteInAppMessages(inApps: List<InAppMessageDb>) {
        /*@formatter:off*/ Logger.i(TAG, "deleteInAppMessages(): ", "inApps = [", inApps, "]")
        /*@formatter:on*/
        val ids = inApps.map { it.messageId }

        for (id: Long in ids) {
            database.delete(
                table = InAppMessageSchema.TABLE_NAME_IN_APP_MESSAGE,
                whereClause = "${InAppMessageSchema.COLUMN_IAM_ID}=?",
                whereArgs = arrayOf(id.toString())
            )
        }
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