package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.displayrules.StringOperator

data class RuleEventParameter(
    val name: String,
    val operator: StringOperator,
    val values: List<String>
)