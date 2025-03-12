package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.model.iam.SegmentDb
import com.reteno.core.util.Util

fun ContentValues.putInAppMessage(inApp: InAppMessageDb) {
    put(InAppMessageSchema.COLUMN_IAM_ID, inApp.messageId)
    put(InAppMessageSchema.COLUMN_IAM_INSTANCE_ID, inApp.messageInstanceId)
    put(InAppMessageSchema.COLUMN_IAM_DISPLAY_RULES, inApp.displayRules)
    put(InAppMessageSchema.COLUMN_IAM_LAST_SHOW_TIME, inApp.lastShowTime)
    put(InAppMessageSchema.COLUMN_IAM_SHOW_COUNT, inApp.showCount)
    put(InAppMessageSchema.COLUMN_IAM_LAYOUT_TYPE, inApp.layoutType)
    put(InAppMessageSchema.COLUMN_IAM_MODEL, inApp.model.toString())
    put(InAppMessageSchema.COLUMN_IAM_POSITION, inApp.position)
}

fun ContentValues.putSegment(parentRowId: Long, segment: SegmentDb) {
    put(InAppMessageSchema.COLUMN_IAM_ROW_ID, parentRowId)

    put(InAppMessageSchema.SegmentSchema.COLUMN_SEGMENT_ID, segment.segmentId)
    put(InAppMessageSchema.SegmentSchema.COLUMN_IS_IN_SEGMENT, segment.isInSegment.toString())
    put(InAppMessageSchema.SegmentSchema.COLUMN_LAST_CHECK_TIME, segment.lastCheckTime)
    put(InAppMessageSchema.SegmentSchema.COLUMN_CHECK_STATUS_CODE, segment.checkStatusCode)
    put(InAppMessageSchema.SegmentSchema.COLUMN_RETRY_AFTER, segment.retryAfter)
}

fun List<InAppMessageDb>.toContentValuesList(): List<ContentValues> {
    val contentValuesList = mutableListOf<ContentValues>()
    this.forEach {
        val contentValues = ContentValues()
        contentValues.putInAppMessage(it)
        contentValuesList.add(contentValues)
    }
    return contentValuesList
}

fun List<Pair<Long, SegmentDb>>.toSegmentContentValuesList(): List<ContentValues> {
    val contentValuesList = mutableListOf<ContentValues>()
    this.forEach { (parentRowId, segment) ->
        val contentValues = ContentValues()
        contentValues.putSegment(parentRowId, segment)
        contentValuesList.add(contentValues)
    }
    return contentValuesList
}

fun Cursor.getInAppMessage(): InAppMessageDb? {
    val segmentId = getLongOrNull(getColumnIndex(InAppMessageSchema.SegmentSchema.COLUMN_SEGMENT_ID))
    val isInSegment = getStringOrNull(getColumnIndex(InAppMessageSchema.SegmentSchema.COLUMN_IS_IN_SEGMENT))
    val lastCheckTime = getLongOrNull(getColumnIndex(InAppMessageSchema.SegmentSchema.COLUMN_LAST_CHECK_TIME))
    val checkStatusCode = getIntOrNull(getColumnIndex(InAppMessageSchema.SegmentSchema.COLUMN_CHECK_STATUS_CODE))
    val retryAfter = getLongOrNull(getColumnIndex(InAppMessageSchema.SegmentSchema.COLUMN_RETRY_AFTER))

    val segment = if (segmentId == null) {
        null
    } else {
        SegmentDb(
            segmentId = segmentId,
            isInSegment = isInSegment?.toBoolean() ?: false,
            lastCheckTime = lastCheckTime,
            checkStatusCode = checkStatusCode,
            retryAfter = retryAfter
        )
    }

    val rowId = getStringOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_ROW_ID))
    val createdAt = getStringOrNull(getColumnIndex(DbSchema.COLUMN_TIMESTAMP))
    val messageId = getLongOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_ID))
    val messageInstanceId = getLongOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_INSTANCE_ID))
    val displayRules = getStringOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_DISPLAY_RULES))
    val lastShowTime = getLongOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_LAST_SHOW_TIME))
    val showCount = getLongOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_SHOW_COUNT)) ?: 0L
    val layoutType = getStringOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_LAYOUT_TYPE))
    val model = getStringOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_MODEL))
    val position = getStringOrNull(getColumnIndex(InAppMessageSchema.COLUMN_IAM_POSITION))

    val result = if (createdAt == null || messageId == null || messageInstanceId == null || displayRules == null) {
        null
    } else {
        InAppMessageDb(
            rowId = rowId,
            createdAt = Util.formatSqlDateToTimestamp(createdAt),
            messageId = messageId,
            messageInstanceId = messageInstanceId,
            displayRules = displayRules,
            lastShowTime = lastShowTime,
            showCount = showCount,
            layoutType = layoutType,
            model = model,
            position = position
        )
    }

    result?.segment = segment

    return result
}