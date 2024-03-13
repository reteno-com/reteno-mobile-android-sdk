package com.reteno.core.data.remote.model.iam.displayrules.frequency

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import java.util.concurrent.TimeUnit

class FrequencyRuleValidator {
    fun checkInAppMatchesFrequencyRules(inAppMessage: InAppMessage, sessionTimeMillis: Long): Boolean {
        val frequencyRules = inAppMessage.displayRules.frequency?.predicates

        if (frequencyRules.isNullOrEmpty()) {
            return true
        }

        var allRulesMatch = true
        frequencyRules.forEach { rule ->
            if (!checkRuleMatch(rule, inAppMessage, sessionTimeMillis)) {
                allRulesMatch = false
                return@forEach
            }
        }

        return allRulesMatch
    }

    private fun checkRuleMatch(rule: FrequencyRule, inAppMessage: InAppMessage, sessionTimeMillis: Long): Boolean {
        return when (rule) {
            FrequencyRule.NoLimit -> true
            FrequencyRule.OncePerApp -> checkCanShowOncePerApp(inAppMessage)
            FrequencyRule.OncePerSession -> checkCanShowOncePerSession(inAppMessage, sessionTimeMillis)
            is FrequencyRule.MinInterval -> checkCanShowMinInterval(inAppMessage, rule.intervalMillis)
            is FrequencyRule.TimesPerTimeUnit -> checkCanShowTimesPerTimeUnit(
                inAppMessage,
                rule.count,
                rule.timeUnit.toMillis(1)
            )
        }
    }

    private fun checkCanShowOncePerApp(inAppMessage: InAppMessage): Boolean {
        return inAppMessage.lastShowTime == null
    }

    private fun checkCanShowOncePerSession(inAppMessage: InAppMessage, sessionTimeMillis: Long): Boolean {
        val lastShowTime = inAppMessage.lastShowTime
        if (lastShowTime == null) {
            return true
        }
        return System.currentTimeMillis() - lastShowTime > sessionTimeMillis
    }

    private fun checkCanShowMinInterval(inAppMessage: InAppMessage, intervalMillis: Long): Boolean {
        val lastShowTime = inAppMessage.lastShowTime
        if (lastShowTime == null) {
            return true
        }
        return System.currentTimeMillis() - lastShowTime > intervalMillis
    }

    private fun checkCanShowTimesPerTimeUnit(inAppMessage: InAppMessage, times: Int, timeUnitMillis: Long): Boolean {
        val lastShowTime: Long = inAppMessage.lastShowTime ?: 0L
        return when {
            inAppMessage.showCount <= 0L -> true
            inAppMessage.showCount >= times -> false
            else -> {
                System.currentTimeMillis() - lastShowTime > timeUnitMillis
            }
        }
    }
}