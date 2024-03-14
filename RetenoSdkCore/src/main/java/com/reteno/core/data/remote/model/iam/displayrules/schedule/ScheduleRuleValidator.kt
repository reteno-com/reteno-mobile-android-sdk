package com.reteno.core.data.remote.model.iam.displayrules.schedule

import com.reteno.core.data.remote.model.iam.message.InAppMessage
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
        val result = showAfter.zonedDateTime.isAfter(now)
        return result
    }

    private fun checkHideAfterRuleMatches(hideAfter: ScheduleRule.HideAfter, now: ZonedDateTime): Boolean {
        val result = hideAfter.zonedDateTime.isBefore(now)
        return result
    }

    private fun checkSpecificDaysAndTimeRuleMatches(
        daysAndTime: ScheduleRule.SpecificDaysAndTime,
        now: ZonedDateTime
    ): Boolean {
        if (daysAndTime.daysOfWeek.isNotEmpty()) {
            val currentDay = now.dayOfWeek.name.lowercase()
            val foundDay = daysAndTime.daysOfWeek.firstOrNull { it == currentDay }
            if (foundDay == null) return false
        }

        if (daysAndTime.fromHours == 0 && daysAndTime.fromMinutes == 0 &&
            daysAndTime.toHours == 0 && daysAndTime.toMinutes == 0) return true

        if (now.hour < daysAndTime.fromHours) return false
        if (now.hour == daysAndTime.fromHours && now.minute < daysAndTime.fromMinutes) return false
        if (now.hour > daysAndTime.toHours) return false
        if (now.hour == daysAndTime.toHours && now.minute > daysAndTime.toMinutes) return false

        return true
    }
}