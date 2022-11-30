package com.reteno.core.data.local.model.appinbox

import com.reteno.core.util.Util

data class AppInboxMessageDb(
    val id: String,
    val deviceId: String,
    val occurredDate: String = Util.getCurrentTimeStamp(),
    val status: AppInboxMessageStatusDb = AppInboxMessageStatusDb.OPENED
)