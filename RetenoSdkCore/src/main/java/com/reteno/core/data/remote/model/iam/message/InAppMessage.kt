package com.reteno.core.data.remote.model.iam.message

import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules

data class InAppMessage(
    val messageId: Long,
    val messageInstanceId: Long,
    val displayRules: DisplayRules,
    var content: InAppMessageContent? = null,
    var alreadyShown: Boolean = false
)