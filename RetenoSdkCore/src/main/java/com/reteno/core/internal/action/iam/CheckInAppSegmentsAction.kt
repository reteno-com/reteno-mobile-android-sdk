package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.mappers.toDomain
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckRequest
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResponse
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResult
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.stepOrNull
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient

internal interface CheckInAppSegments {
    suspend operator fun invoke(messages: List<InAppMessage>, persist: Boolean): Result<Unit>
}

internal class CheckInAppSegmentsAction(
    private val api: SuspendApiClient,
    private val cache: InAppMessagesCache,
    private val clock: () -> Long,
    private val runner: ActionRunner
) : CheckInAppSegments {

    override suspend operator fun invoke(
        messages: List<InAppMessage>,
        persist: Boolean
    ): Result<Unit> = runner("CheckInAppSegments") {
        args("messages" to messages.size, "persist" to persist)
        check(messages, persist)
    }

    private suspend fun ActionTrace.check(messages: List<InAppMessage>, persist: Boolean) {
        val segmented = messages.mapNotNull { segmentedOrNull(it) }
        if (segmented.isEmpty()) {
            note { "no in-app message has a segment rule" }
            return
        }
        val checks = loadChecks(segmented.map { it.segment.segmentId }.distinct())
        val checked = step<List<InAppMessage>>(
            "apply segment checks to in-app messages",
            detail = { "count=${it.size}" }
        ) {
            segmented.filter { apply(it.segment, checks) }.map { it.message }
        }
        if (persist && checked.isNotEmpty()) {
            step("save checked in-app messages to local database") {
                cache.update(checked)
            }
        }
    }

    private suspend fun ActionTrace.loadChecks(segmentIds: List<Long>): List<AsyncRulesCheckResult> {
        val response = step("check user segments on server") {
            api.request(
                HttpMethod.POST,
                ApiContract.InAppMessages.CheckUserInSegments,
                AsyncRulesCheckRequest.createSegmentRequest(segmentIds).toJson()
            )
        }
        if (response is ApiResult.Failure) return emptyList()
        val body = (response as ApiResult.Success).body
        val checks = stepOrNull("read segment checks from server response") {
            body.fromJson<AsyncRulesCheckResponse>().checks
        }
        return checks.orEmpty()
    }

    private fun segmentedOrNull(message: InAppMessage): SegmentedMessage? {
        val segment = message.displayRules.async?.segment ?: return null
        return SegmentedMessage(message, segment)
    }

    private fun apply(segment: SegmentRule, checks: List<AsyncRulesCheckResult>): Boolean {
        val check = checks.firstOrNull { it.segmentId == segment.segmentId } ?: return false
        segment.isInSegment = check.checkResult ?: false
        segment.retryParams = check.error?.toDomain()
        segment.lastCheckedTimestamp = clock()
        return true
    }

    private class SegmentedMessage(val message: InAppMessage, val segment: SegmentRule)
}
