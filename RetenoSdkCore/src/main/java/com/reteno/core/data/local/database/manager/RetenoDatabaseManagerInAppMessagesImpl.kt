package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema.SegmentSchema
import com.reteno.core.data.local.database.util.getInAppMessage
import com.reteno.core.data.local.database.util.toContentValuesList
import com.reteno.core.data.local.database.util.toSegmentContentValuesList
import com.reteno.core.data.local.model.iam.InAppMessageDb

internal class RetenoDatabaseManagerInAppMessagesImpl(
    private val database: RetenoDatabase
) : RetenoDatabaseManagerInAppMessages {

    override fun insertInAppMessages(inApps: List<InAppMessageDb>) {
        val rowIds = database.insertMultiple(
            table = InAppMessageSchema.TABLE_NAME,
            contentValues = inApps.toContentValuesList()
        )
        if (rowIds.size != inApps.size) return
        val segments = rowIds.zip(inApps).mapNotNull { (rowId, inApp) -> inApp.segment?.let { rowId to it } }
        database.insertMultiple(
            table = SegmentSchema.TABLE_NAME_SEGMENT,
            contentValues = segments.toSegmentContentValuesList()
        )
    }

    override fun getInAppMessages(limit: Int?): List<InAppMessageDb> = database.readRows(
        table = InAppMessageSchema.TABLE_NAME,
        rowIdColumn = InAppMessageSchema.COLUMN_IAM_ROW_ID,
        query = { rawQuery(inAppMessagesQuery(limit)) },
        read = { getInAppMessage() }
    )

    private fun inAppMessagesQuery(limit: Int?): String {
        val message = InAppMessageSchema.TABLE_NAME
        val segment = SegmentSchema.TABLE_NAME_SEGMENT
        val columns = listOf(
            message to InAppMessageSchema.COLUMN_IAM_ROW_ID,
            message to InAppMessageSchema.COLUMN_IAM_ID,
            message to InAppMessageSchema.COLUMN_IAM_INSTANCE_ID,
            message to DbSchema.COLUMN_TIMESTAMP,
            message to InAppMessageSchema.COLUMN_IAM_DISPLAY_RULES,
            message to InAppMessageSchema.COLUMN_IAM_LAST_SHOW_TIME,
            message to InAppMessageSchema.COLUMN_IAM_SHOW_COUNT,
            message to InAppMessageSchema.COLUMN_IAM_LAYOUT_TYPE,
            message to InAppMessageSchema.COLUMN_IAM_MODEL,
            message to InAppMessageSchema.COLUMN_IAM_POSITION,
            segment to SegmentSchema.COLUMN_SEGMENT_ID,
            segment to SegmentSchema.COLUMN_IS_IN_SEGMENT,
            segment to SegmentSchema.COLUMN_LAST_CHECK_TIME,
            segment to SegmentSchema.COLUMN_CHECK_STATUS_CODE,
            segment to SegmentSchema.COLUMN_RETRY_AFTER
        )
        val rowId = InAppMessageSchema.COLUMN_IAM_ROW_ID
        return selectColumns(columns) +
            " FROM $message" +
            " LEFT JOIN $segment ON $message.$rowId = $segment.$rowId" +
            limitClause(limit)
    }

    override fun getInAppMessagesCount(): Long = database.getRowCount(InAppMessageSchema.TABLE_NAME)

    override fun deleteInAppMessages(inApps: List<InAppMessageDb>) {
        inApps.forEach {
            database.delete(
                table = InAppMessageSchema.TABLE_NAME,
                whereClause = "${InAppMessageSchema.COLUMN_IAM_ID}=?",
                whereArgs = arrayOf(it.messageId.toString())
            )
        }
    }

    override fun deleteAllInAppMessages() {
        database.delete(table = InAppMessageSchema.TABLE_NAME)
    }
}
