package com.reteno.core.data.remote.model.iam.displayrules

import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingDisplayRules

data class DisplayRules(
    val frequency: FrequencyDisplayRules?,
    val targeting: TargetingDisplayRules?,
    val schedule: ScheduleDisplayRules?,
    val async: AsyncDisplayRules?
)