package com.reteno.core.lifecycle

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.LifecycleTrackingOptions

interface RetenoSessionHandler {

    fun getForegroundTimeMillis(): Long
    fun getSessionStartTimestamp(): Long
    fun getSessionId(): String
    fun setLifecycleEventConfig(lifecycleEventConfig: LifecycleTrackingOptions)
    fun start()
    fun stop()
    fun scheduleInAppMessages(
        messages: MutableList<InAppWithTime>,
        onTimeMatch: (List<InAppMessage>) -> Unit
    )
    fun clearSessionForced()

}