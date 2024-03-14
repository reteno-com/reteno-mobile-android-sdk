package com.reteno.core.data.local.mappers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRulesParsingException
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.util.InAppMessageUtil

internal fun InAppMessageResponse.toInAppMessage() = InAppMessage(
    messageId = messageId,
    messageInstanceId = messageInstanceId,
    displayRulesJson = displayRules,
    displayRules = InAppMessageUtil.parseRules(displayRules)
)

internal fun List<InAppMessageResponse>.mapResponseToInAppMessages(): List<InAppMessage> {
    return mapNotNull { message ->
        try {
            message.toInAppMessage()
        } catch (e: DisplayRulesParsingException) {
            e.printStackTrace()
            null
        }
    }
}

internal fun InAppMessageDb.toInAppMessage(): InAppMessage {
    val rulesJson = displayRules.fromJson<JsonObject>()
    val content = if (model != null && layoutType != null) {
        InAppMessageContent(
            messageInstanceId = messageInstanceId,
            layoutType = layoutType,
            model = model.fromJson<JsonElement>()
        )
    } else null

    return InAppMessage(
        messageId = messageId,
        messageInstanceId = messageInstanceId,
        displayRulesJson = rulesJson,
        displayRules = InAppMessageUtil.parseRules(rulesJson),
        content = content,
        lastShowTime = lastShowTime,
        showCount = showCount
    )
}

internal fun List<InAppMessageDb>.mapDbToInAppMessages(): List<InAppMessage> {
    return mapNotNull { message ->
        try {
            message.toInAppMessage()
        } catch (e: DisplayRulesParsingException) {
            e.printStackTrace()
            null
        }
    }
}

internal fun InAppMessage.toDB() = InAppMessageDb(
    messageId = messageId,
    messageInstanceId = messageInstanceId,
    displayRules = displayRulesJson.toString(),
    layoutType = content?.layoutType,
    model = content?.model?.toString(),
    lastShowTime = lastShowTime,
    showCount = showCount
)