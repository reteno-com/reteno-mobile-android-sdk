package com.reteno.core.fixtures

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent

internal fun stubInAppContent(model: JsonElement = JsonObject()) = InAppMessageContent(
    messageInstanceId = 1,
    layoutType = InAppMessageContent.InAppLayoutType.FULL,
    layoutParams = null,
    model = model
)

internal fun stubInAppMessage(
    messageId: Long = 1,
    content: InAppMessageContent? = stubInAppContent(),
    frequency: List<FrequencyRule>? = null,
    schedule: List<ScheduleRule>? = null,
    async: AsyncDisplayRules? = null,
    lastShowTime: Long? = null,
    showCount: Long = 0
) = InAppMessage(
    messageId = messageId,
    messageInstanceId = messageId,
    displayRulesJson = JsonObject(),
    displayRules = DisplayRules(
        frequency = frequency?.let { rules -> FrequencyDisplayRules().apply { predicates.addAll(rules) } },
        targeting = null,
        schedule = schedule?.let { rules -> ScheduleDisplayRules().apply { predicates.addAll(rules) } },
        async = async
    ),
    content = content,
    lastShowTime = lastShowTime,
    showCount = showCount
)

internal fun stubSegment(inSegment: Boolean) = SegmentRule(segmentId = 1).apply { isInSegment = inSegment }

internal fun stubEventRule(
    name: String = "purchase",
    relation: RuleRelation = RuleRelation.AND,
    params: List<RuleEventParameter>? = null
) = TargetingRule.Event(name = name, paramsRelation = relation, params = params)

internal fun stubInAppWithEvent(
    messageId: Long = 1,
    rule: TargetingRule.Event = stubEventRule()
) = InAppWithEvent(inApp = stubInAppMessage(messageId = messageId), event = rule)

internal fun stubDaysAndTime(
    timeZone: String = "Europe/Paris",
    days: List<String> = emptyList(),
    from: Pair<Int, Int> = 0 to 0,
    to: Pair<Int, Int> = 0 to 0
) = ScheduleRule.SpecificDaysAndTime(
    timeZone = timeZone,
    daysOfWeek = days,
    fromHours = from.first,
    fromMinutes = from.second,
    toHours = to.first,
    toMinutes = to.second
)
