package com.reteno.core.data.remote.model.iam.message

import com.google.gson.JsonObject
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules

data class InAppMessage(
    val messageId: Long,
    val messageInstanceId: Long,
    val displayRulesJson: JsonObject,
    val displayRules: DisplayRules,
    var content: InAppMessageContent? = null,
    var lastShowTime: Long? = null,
    var showCount: Long = 0
) {
    fun notifyShown() {
        lastShowTime = System.currentTimeMillis()
        showCount++
    }
}