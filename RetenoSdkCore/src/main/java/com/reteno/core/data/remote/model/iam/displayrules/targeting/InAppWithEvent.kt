package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.message.InAppMessage

data class InAppWithEvent (
    val inApp: InAppMessage,
    val event: TargetingRule.Event
)