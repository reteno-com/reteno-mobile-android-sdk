package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.message.InAppMessage

data class InAppWithTime(
    val inApp: InAppMessage,
    val time: Long
)