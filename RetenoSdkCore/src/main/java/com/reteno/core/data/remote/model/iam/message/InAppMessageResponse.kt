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
)