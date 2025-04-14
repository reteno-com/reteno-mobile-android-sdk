package com.reteno.core.data.remote.model.iam.displayrules.schedule

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import java.time.LocalTime
import java.time.ZonedDateTime

class ScheduleRuleValidator {
    fun checkInAppMatchesScheduleRules(inAppMessage: InAppMessage): Boolean {
        val scheduleRules = inAppMessage.displayRules.schedule?.predicates

        if (scheduleRules.isNullOrEmpty()) {
            return true
        }

        var allRulesMatch = true
        val now = ZonedDateTime.now()
        scheduleRules.forEach { rule ->
            if (!checkRuleMatch(rule, now)) {
                allRulesMatch = false
                return@forEach
            }
        }

        return allRulesMatch
    }

    private fun checkRuleMatch(scheduleRule: ScheduleRule, now: ZonedDateTime): Boolean {
        return when (scheduleRule) {
            is ScheduleRule.ShowAfter -> checkShowAfterRuleMatches(scheduleRule, now)
            is ScheduleRule.HideAfter -> checkHideAfterRuleMatches(scheduleRule, now)
            is ScheduleRule.SpecificDaysAndTime -> checkSpecificDaysAndTimeRuleMatches(scheduleRule, now)
        }
    }

    private fun checkShowAfterRuleMatches(showAfter: ScheduleRule.ShowAfter, now: ZonedDateTime): Boolean {
        val result = showAfter.zonedDateTime.isBefore(now)
        return result
    }

    private fun checkHideAfterRuleMatches(hideAfter: ScheduleRule.HideAfter, now: ZonedDateTime): Boolean {
        val result = hideAfter.zonedDateTime.isAfter(now)
        return result
    }

    private fun checkSpecificDaysAndTimeRuleMatches(
        daysAndTime: ScheduleRule.SpecificDaysAndTime,
        now: ZonedDateTime
    ): Boolean {
        if (daysAndTime.daysOfWeek.isNotEmpty()) {
            val currentDay = now.dayOfWeek.name.lowercase()
            daysAndTime.daysOfWeek.firstOrNull { it == currentDay } ?: return false
        }

        if (daysAndTime.fromHours == 0 && daysAndTime.fromMinutes == 0 &&
            daysAndTime.toHours == 0 && daysAndTime.toMinutes == 0) return true

        val localNow = now.toLocalTime()
        val startTime = LocalTime.of(daysAndTime.fromHours, daysAndTime.fromMinutes)
        val endTime = LocalTime.of(daysAndTime.toHours, daysAndTime.toMinutes, 59)

        if (startTime.isAfter(endTime)) {
            val dayEndTime = LocalTime.of(23,59, 59)
            val dayStartTime = LocalTime.ofSecondOfDay(0)
            val inFirstRange = !localNow.isBefore(startTime) && localNow.isBefore(dayEndTime)
            val inSecondRange = !localNow.isBefore(dayStartTime) && localNow.isBefore(endTime)
            return inFirstRange || inSecondRange
        }

        return !localNow.isBefore(startTime) && localNow.isBefore(endTime)
    }
}