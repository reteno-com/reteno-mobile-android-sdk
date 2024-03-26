package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation

data class TargetingRuleConditionsGroup(
    val relation: RuleRelation,
    val conditions: List<TargetingRule>
)