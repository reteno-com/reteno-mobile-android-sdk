package com.reteno.core.data.local.mappers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.Instant
import java.util.UUID
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.model.iam.SegmentDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRuleRetryParams
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckError
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.util.InAppMessageUtil
import com.reteno.core.util.Logger
import com.reteno.core.util.toTimeUnit

internal fun InAppMessageResponse.toInAppMessage(): InAppMessage {
    if (inWorkflow) {
        require(!iid.isNullOrBlank())
        require(UUID.fromString(iid).toString().equals(iid, ignoreCase = true))
        Instant.parse(requireNotNull(expiresAt))
        require(InAppMessageUtil.supportsWorkflowRules(displayRules))
    }
    return InAppMessage(
        messageId = messageId,
        messageInstanceId = messageInstanceId,
        displayRulesJson = displayRules,
        displayRules = InAppMessageUtil.parseRules(
            if (inWorkflow) displayRules.deepCopy().apply { remove("FREQUENCY") } else displayRules
        ),
        inWorkflow = inWorkflow,
        iid = iid.takeIf { inWorkflow },
        expiresAt = expiresAt.takeIf { inWorkflow }
    )
}

internal fun List<InAppMessageResponse>.mapResponseToInAppMessages(): List<InAppMessage> {
    return mapNotNull { message ->
        try {
            message.toInAppMessage()
        } catch (e: Throwable) {
            Logger.w("InAppMessageMapper", "Unsupported In-app entry deferred")
            null
        }
    }
}

internal fun InAppMessageDb.toInAppMessage(): InAppMessage {
    val rulesJson = displayRules.fromJson<JsonObject>()
    val content = if (model != null && layoutType != null) {
        InAppMessageContent(
            messageInstanceId = messageInstanceId,
            layoutType = InAppLayoutType.from(layoutType),
            layoutParams = position?.let { InAppLayoutParams(InAppLayoutParams.Position.from(it)) },
            model = model.fromJson<JsonElement>()
        )
    } else null

    val result = InAppMessage(
        messageId = messageId,
        messageInstanceId = messageInstanceId,
        displayRulesJson = rulesJson,
        displayRules = InAppMessageUtil.parseRules(if (iid != null) rulesJson.deepCopy().apply { remove("FREQUENCY") } else rulesJson),
        content = content,
        lastShowTime = lastShowTime,
        showCount = showCount,
        inWorkflow = iid != null,
        iid = iid,
        expiresAt = expiresAt,
        workflowContext = workflowContext
    )

    segment?.let {
        val resultSegment = result.displayRules.async?.segment
        if (resultSegment != null && resultSegment.segmentId == it.segmentId) {
            result.displayRules.async.segment = it.toDomain()
        }
    }

    return result
}

internal fun List<InAppMessageDb>.mapDbToInAppMessages(): List<InAppMessage> {
    return mapNotNull { message ->
        try {
            message.toInAppMessage()
        } catch (e: Throwable) {
            Logger.w("InAppMessageMapper", "Unsupported In-app entry deferred")
            null
        }
    }
}

internal fun InAppMessage.toDB(): InAppMessageDb {
    val result = InAppMessageDb(
        messageId = messageId,
        messageInstanceId = messageInstanceId,
        displayRules = displayRulesJson.toString(),
        layoutType = content?.layoutType?.key.takeUnless { inWorkflow },
        model = content?.model?.toString().takeUnless { inWorkflow },
        position = content?.layoutParams?.position?.key,
        lastShowTime = lastShowTime,
        showCount = showCount,
        iid = iid,
        expiresAt = expiresAt,
        workflowContext = workflowContext
    )

    result.segment = displayRules.async?.segment?.toDb()

    return result
}

internal fun InAppMessage.updateFromDb(messageDb: InAppMessageDb) {
    lastShowTime = messageDb.lastShowTime
    showCount = messageDb.showCount
    displayRules.async?.segment?.let {
        val segmentDb = messageDb.segment
        if (segmentDb != null && it.segmentId == segmentDb.segmentId) {
            it.isInSegment = segmentDb.isInSegment
            it.lastCheckedTimestamp = segmentDb.lastCheckTime
            it.retryParams?.statusCode = segmentDb.checkStatusCode ?: 0
            it.retryParams?.retryAfter = segmentDb.retryAfter
        }
    }
}

internal fun AsyncRulesCheckError.toDomain() = AsyncRuleRetryParams(
    statusCode = statusCode,
    retryAfter = retryAfter?.let { retryModel ->
        retryModel.timeUnit.toTimeUnit()?.toMillis(retryModel.amount ?: 0)
    }
)

internal fun SegmentRule.toDb() = SegmentDb(
    segmentId = segmentId,
    isInSegment = isInSegment,
    lastCheckTime = lastCheckedTimestamp,
    checkStatusCode = retryParams?.statusCode,
    retryAfter = retryParams?.retryAfter
)

internal fun SegmentDb.toDomain(): SegmentRule {
    val result = SegmentRule(segmentId)
    result.isInSegment = isInSegment
    result.lastCheckedTimestamp = lastCheckTime
    result.retryParams = checkStatusCode?.let { code ->
        AsyncRuleRetryParams(code, retryAfter)
    }
    return result
}