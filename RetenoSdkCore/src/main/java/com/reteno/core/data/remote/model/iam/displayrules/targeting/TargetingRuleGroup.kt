package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation

data class TargetingRuleGroup(
    val relation: RuleRelation,
    val groups: List<TargetingRuleConditionsGroup>
)