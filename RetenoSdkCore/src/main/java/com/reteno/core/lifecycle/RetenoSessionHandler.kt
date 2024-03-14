package com.reteno.core.lifecycle

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage

interface RetenoSessionHandler {
    fun getForegroundTimeMillis(): Long
    fun start()
    fun stop()
    fun scheduleInAppMessages(
        messages: MutableList<InAppWithTime>,
        onTimeMatch: (List<InAppMessage>) -> Unit
    )

}