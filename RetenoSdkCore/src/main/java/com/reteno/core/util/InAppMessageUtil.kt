package com.reteno.core.util

import com.google.gson.JsonObject
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRuleType
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRulesParsingException
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleConditionsGroup
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleGroup

object InAppMessageUtil {
    fun parseRules(displayRulesJson: JsonObject): DisplayRules {
        return DisplayRules(
            parseFrequencyRules(displayRulesJson),
            parseTargetingRules(displayRulesJson),
            parseScheduleRules(displayRulesJson),
            parseAsyncRules(displayRulesJson)
        )
    }

    private fun parseFrequencyRules(displayRulesJson: JsonObject): FrequencyDisplayRules {
        val frequencyRules = FrequencyDisplayRules()
        val frequency: JsonObject? = displayRulesJson.getAsJsonObject(DisplayRuleType.FREQUENCY.name)

        if (frequency != null && frequency.get("enabled")?.asBoolean == true) {
            val predicates = frequency.getAsJsonArray("predicates")
            predicates.forEach { item ->
                val rule = FrequencyRule.fromJson(item.asJsonObject)
                if (rule != null) {
                    frequencyRules.predicates.add(rule)
                }
            }
        }

        return frequencyRules
    }

    private fun parseTargetingRules(displayRulesJson: JsonObject): TargetingDisplayRules? {
        val targeting: JsonObject? = displayRulesJson.getAsJsonObject(DisplayRuleType.TARGETING.name)

        return if (targeting != null && targeting.get("enabled")?.asBoolean == true) {
            val schema = targeting.getAsJsonObject("schema")
            val includeGroup = parseTargetingGroup(schema.getAsJsonObject("include"))
            val excludeGroup = parseTargetingGroup(schema.getAsJsonObject("exclude"))

            if (includeGroup != null || excludeGroup != null) {
                TargetingDisplayRules(includeGroup, excludeGroup)
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun parseTargetingGroup(groupJson: JsonObject): TargetingRuleGroup? {
        val groupsRelation = RuleRelation.fromString(groupJson.get("relation").asString)
        val conditionsGroups = mutableListOf<TargetingRuleConditionsGroup>()

        groupJson.getAsJsonArray("groups").forEach {
            val group = parseTargetingConditions(it.asJsonObject)
            if (group != null) {
                conditionsGroups.add(group)
            }
        }

        return if (groupsRelation != null && conditionsGroups.isNotEmpty()) {
            TargetingRuleGroup(groupsRelation, conditionsGroups)
        } else {
            null
        }
    }

    private fun parseTargetingConditions(conditionsGroup: JsonObject): TargetingRuleConditionsGroup? {
        val conditionsRelation = RuleRelation.fromString(conditionsGroup.get("relation").asString)
        val conditions = mutableListOf<TargetingRule>()

        conditionsGroup.getAsJsonArray("conditions").forEach {
            val condition = TargetingRule.fromJson(it.asJsonObject)
            if (condition != null) {
                conditions.add(condition)
            }
        }

        return if (conditionsRelation != null && conditions.isNotEmpty()) {
            TargetingRuleConditionsGroup(conditionsRelation, conditions)
        } else {
            null
        }
    }

    private fun parseAsyncRules(displayRulesJson: JsonObject): AsyncDisplayRules? {
        val async: JsonObject? = displayRulesJson.getAsJsonObject(DisplayRuleType.ASYNC.name)
        val asyncKeys = async?.keySet()
        val asyncRules = when {
            asyncKeys == null -> null
            asyncKeys.isEmpty() -> null
            asyncKeys.size == 1 -> {
                val segmentJson = async.getAsJsonObject("IS_IN_SEGMENT")
                if (segmentJson == null) {
                    throw DisplayRulesParsingException()
                }
                if (segmentJson.get("enabled")?.asBoolean == true) {
                    val segmentId = segmentJson.get("segmentId").asLong
                    AsyncDisplayRules(SegmentRule(segmentId))
                } else {
                    null
                }
            }
            else -> throw DisplayRulesParsingException()
        }
        return asyncRules
    }



    private fun parseScheduleRules(displayRulesJson: JsonObject): ScheduleDisplayRules? {
        val scheduleRules = ScheduleDisplayRules()
        val schedule: JsonObject? = displayRulesJson.getAsJsonObject(DisplayRuleType.SCHEDULE.name)

        if (schedule != null && schedule.get("enabled")?.asBoolean == true) {
            val predicates = schedule.getAsJsonArray("predicates")
            predicates.forEach { item ->
                val rule = ScheduleRule.fromJson(item.asJsonObject)
                if (rule != null) {
                    scheduleRules.predicates.add(rule)
                }
            }
        }

        return if (scheduleRules.predicates.isEmpty()) {
            null
        } else {
            scheduleRules
        }
    }
}