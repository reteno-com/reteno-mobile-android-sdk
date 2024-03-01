package com.reteno.core.data.remote.model.iam.message

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRuleType
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.ScheduleDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleConditionsGroup
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleGroup

data class InAppMessageResponse(
    @SerializedName("messageId")
    val messageId: Long,
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,
    @SerializedName("displayRules")
    val displayRules: JsonObject,
) {
    fun parseRules(): DisplayRules {
        return DisplayRules(
            parseFrequencyRules(),
            parseTargetingRules(),
            null,
            parseAsyncRules()
        )
    }

    private fun parseFrequencyRules(): FrequencyDisplayRules {
        val frequencyRules = FrequencyDisplayRules()
        val frequency: JsonObject? = displayRules.getAsJsonObject(DisplayRuleType.FREQUENCY.name)

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

    private fun parseTargetingRules(): TargetingDisplayRules? {
        val targeting: JsonObject? = displayRules.getAsJsonObject(DisplayRuleType.TARGETING.name)

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

    private fun parseAsyncRules(): AsyncDisplayRules? {
        val async: JsonObject? = displayRules.getAsJsonObject(DisplayRuleType.ASYNC.name)
        val segment = async?.getAsJsonObject("IS_IN_SEGMENT")

        return if (segment != null && segment.get("enabled")?.asBoolean == true) {
            val segmentId = segment.get("segmentId").asLong
            AsyncDisplayRules(SegmentRule(segmentId))
        } else {
            null
        }
    }
}