package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.util.allElementsNotNull

fun ContentValues.putAppInbox(inboxDb: AppInboxMessageDb) {
    put(AppInboxSchema.COLUMN_APP_INBOX_ID, inboxDb.id)
    put(AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID, inboxDb.deviceId)
    put(AppInboxSchema.COLUMN_APP_INBOX_STATUS, inboxDb.status.toString())
    put(AppInboxSchema.COLUMN_APP_INBOX_TIME, inboxDb.occurredDate)
}

fun Cursor.getAppInbox(): AppInboxMessageDb? {
    val id = getStringOrNull(getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_ID))
    val deviceId = getStringOrNull(getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID))
    val time = getStringOrNull(getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_TIME))
    val status = getStringOrNull(getColumnIndex(AppInboxSchema.COLUMN_APP_INBOX_STATUS))

    return if (allElementsNotNull(id, deviceId, time, status)) {
        AppInboxMessageDb(
            id = id!!,
            deviceId = deviceId!!,
            occurredDate = time!!,
            status = AppInboxMessageStatusDb.fromString(status)
        )
    } else {
        null
    }
}