package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.internal.action.trace.ActionTraceImpl
import com.reteno.core.internal.action.trace.TracedOutcome
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.regex.PatternSyntaxException

internal enum class DisplayDecision(override val isSuccessful: Boolean) : TracedOutcome {
    SHOW(true),
    NO_CONTENT(false),
    NOT_IN_SEGMENT(false),
    FREQUENCY_LIMIT_REACHED(false),
    OUTSIDE_SCHEDULE(false)
}

internal interface InAppDisplayRules {
    fun decide(message: InAppMessage, sessionStartTimestamp: Long, showingOnAppStart: Boolean): DisplayDecision

    fun matchesEvent(rule: TargetingRule.Event, event: Event): Boolean
}

internal class DisplayRulesEvaluator(
    private val clock: () -> Long,
    private val sink: RetenoLogSink
) : InAppDisplayRules {

    override fun decide(
        message: InAppMessage,
        sessionStartTimestamp: Long,
        showingOnAppStart: Boolean
    ): DisplayDecision {
        if (!hasContent(message)) return DisplayDecision.NO_CONTENT
        if (!isInSegment(message)) return DisplayDecision.NOT_IN_SEGMENT
        if (!matchesFrequency(message, sessionStartTimestamp, showingOnAppStart)) {
            return DisplayDecision.FREQUENCY_LIMIT_REACHED
        }
        if (!matchesSchedule(message)) return DisplayDecision.OUTSIDE_SCHEDULE
        return DisplayDecision.SHOW
    }

    override fun matchesEvent(rule: TargetingRule.Event, event: Event): Boolean {
        if (rule.name != event.eventTypeKey) return false
        val parameters = rule.params ?: return true
        if (parameters.isEmpty()) return true
        if (rule.paramsRelation == RuleRelation.OR) {
            return parameters.any { matchesParameter(it, event.params) }
        }
        return parameters.all { matchesParameter(it, event.params) }
    }

    private fun hasContent(message: InAppMessage): Boolean {
        val content = message.content ?: return false
        return !content.model.isJsonNull
    }

    private fun isInSegment(message: InAppMessage): Boolean {
        val segment = message.displayRules.async?.segment ?: return true
        return segment.isInSegment
    }

    private fun matchesFrequency(
        message: InAppMessage,
        sessionStartTimestamp: Long,
        showingOnAppStart: Boolean
    ): Boolean {
        val frequency = message.displayRules.frequency ?: return true
        return frequency.predicates.all { matchesFrequencyRule(it, message, sessionStartTimestamp, showingOnAppStart) }
    }

    private fun matchesFrequencyRule(
        rule: FrequencyRule,
        message: InAppMessage,
        sessionStartTimestamp: Long,
        showingOnAppStart: Boolean
    ): Boolean = when (rule) {
        FrequencyRule.NoLimit -> !showingOnAppStart || notShownInSession(message, sessionStartTimestamp)
        FrequencyRule.OncePerApp -> message.lastShowTime == null
        FrequencyRule.OncePerSession -> notShownInSession(message, sessionStartTimestamp)
        is FrequencyRule.MinInterval -> intervalPassedSinceLastShow(message, rule.intervalMillis)
        is FrequencyRule.TimesPerTimeUnit -> matchesTimesPerTimeUnit(message, rule)
    }

    private fun notShownInSession(message: InAppMessage, sessionStartTimestamp: Long): Boolean {
        val lastShowTime = message.lastShowTime ?: return true
        return lastShowTime < sessionStartTimestamp
    }

    private fun intervalPassedSinceLastShow(message: InAppMessage, intervalMillis: Long): Boolean {
        val lastShowTime = message.lastShowTime ?: return true
        return clock() - lastShowTime > intervalMillis
    }

    private fun matchesTimesPerTimeUnit(message: InAppMessage, rule: FrequencyRule.TimesPerTimeUnit): Boolean {
        if (message.showCount < rule.count) return true
        val lastShowTime = message.lastShowTime ?: return true
        return clock() - lastShowTime > rule.timeUnit.toMillis(1)
    }

    private fun matchesSchedule(message: InAppMessage): Boolean {
        val schedule = message.displayRules.schedule ?: return true
        val now = Instant.ofEpochMilli(clock())
        return schedule.predicates.all { matchesScheduleRule(it, now) }
    }

    private fun matchesScheduleRule(rule: ScheduleRule, now: Instant): Boolean = when (rule) {
        is ScheduleRule.ShowAfter -> rule.zonedDateTime.toInstant().isBefore(now)
        is ScheduleRule.HideAfter -> rule.zonedDateTime.toInstant().isAfter(now)
        is ScheduleRule.SpecificDaysAndTime -> matchesDaysAndTime(rule, now.atZone(zoneOf(rule)))
    }

    private fun zoneOf(rule: ScheduleRule.SpecificDaysAndTime): ZoneId =
        try {
            ZoneId.of(rule.timeZone)
        } catch (error: DateTimeException) {
            ZoneId.systemDefault()
        }

    private fun matchesDaysAndTime(rule: ScheduleRule.SpecificDaysAndTime, now: ZonedDateTime): Boolean {
        val today = now.dayOfWeek.name.lowercase()
        if (rule.daysOfWeek.isNotEmpty() && today !in rule.daysOfWeek) return false
        val start = LocalTime.of(rule.fromHours, rule.fromMinutes)
        val endMinute = LocalTime.of(rule.toHours, rule.toMinutes)
        if (start == LocalTime.MIDNIGHT && endMinute == LocalTime.MIDNIGHT) return true
        val time = now.toLocalTime()
        val end = endMinute.withSecond(LAST_SECOND)
        if (start.isAfter(end)) {
            val lastSecondOfDay = LocalTime.of(LAST_HOUR, LAST_MINUTE, LAST_SECOND)
            val untilMidnight = !time.isBefore(start) && time.isBefore(lastSecondOfDay)
            return untilMidnight || time.isBefore(end)
        }
        return !time.isBefore(start) && time.isBefore(end)
    }

    private fun matchesParameter(rule: RuleEventParameter, eventParameters: List<Parameter>?): Boolean {
        val value = eventParameters?.firstOrNull { it.name == rule.name }?.value
        return when (rule.operator) {
            StringOperator.EQUALS -> value in rule.values
            StringOperator.DOES_NOT_EQUAL -> value !in rule.values
            StringOperator.CONTAINS,
            StringOperator.CONTAINS_ONE_OF -> value != null && rule.values.any { value.contains(it) }
            StringOperator.STARTS_WITH -> value != null && rule.values.any { value.startsWith(it, ignoreCase = true) }
            StringOperator.ENDS_WITH -> value != null && rule.values.any { value.endsWith(it, ignoreCase = true) }
            StringOperator.REG_EX -> value != null && rule.values.any { matchesPattern(rule.name, it, value) }
        }
    }

    private fun matchesPattern(parameterName: String, pattern: String, value: String): Boolean =
        try {
            Regex(pattern).matches(value)
        } catch (error: PatternSyntaxException) {
            sink.log(LogPriority.WARN, ActionTraceImpl.TAG) {
                "in-app event rule parameter '$parameterName' has an invalid pattern, treated as no match"
            }
            false
        }

    private companion object {
        const val LAST_HOUR = 23
        const val LAST_MINUTE = 59
        const val LAST_SECOND = 59
    }
}
