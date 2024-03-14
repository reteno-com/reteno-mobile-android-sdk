package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.google.gson.JsonObject
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRulesParsingException
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.util.toTimeUnit

sealed class TargetingRule {
    class TimeSpentInApp(
        val timeSpentMillis: Long
    ) : TargetingRule()

    class Event(
        val name: String,
        val paramsRelation: RuleRelation,
        val params: List<RuleEventParameter>? = null
    ) : TargetingRule()

    companion object {
        fun fromJson(json: JsonObject): TargetingRule? {
            val operandJson = json.getAsJsonObject("operand")
            val operandName = operandJson.get("name").asString

            return when (operandName) {
                OPERAND_USER -> {
                    val operator = json.get("operator").asString
                    if (operator == OPERATOR_TIMER_SPENT_IN_APP) {
                        val values = json.getAsJsonArray("values")[0].asJsonObject
                        val timeUnit = values.get("unit").asString.toTimeUnit()
                        val amount = values.get("amount").asLong
                        val amountMillis = timeUnit?.toMillis(amount)

                        if (amountMillis != null) {
                            TimeSpentInApp(amountMillis)
                        } else {
                            null
                        }
                    } else {
                        throw DisplayRulesParsingException()
                    }
                }
                OPERAND_EVENT -> {
                    val values = json.getAsJsonArray("values")[0].asJsonObject
                    val eventName = values.get("event").asString
                    val parameters = values.getAsJsonObject("parameters")
                    val paramsRelation = RuleRelation.fromString(parameters.get("relation").asString)
                    val parameterValues = parameters.getAsJsonArray("values")

                    var eventParams: MutableList<RuleEventParameter>? = null
                    if (parameterValues.isEmpty.not()) {
                        eventParams = mutableListOf()
                        parameterValues.forEach {
                            val paramObject = it.asJsonObject
                            val paramName = paramObject.get("name").asString
                            val paramOperator =
                                StringOperator.fromString(paramObject.get("operator").asString)
                            val paramValues = paramObject.getAsJsonArray("value")
                            paramValues.asList().map { it.asString }
                            if (paramOperator != null) {
                                val paramModel = RuleEventParameter(paramName, paramOperator, paramValues.asList().map { it.asString })
                                eventParams.add(paramModel)
                            }
                        }
                    }

                    if (eventParams != null && paramsRelation == null) {
                        return null
                    }

                    if (eventName != null) {
                        Event(eventName, paramsRelation ?: RuleRelation.AND, eventParams)
                    } else {
                        null
                    }
                }
                else -> {
                    throw DisplayRulesParsingException()
                }
            }
        }

        private const val OPERAND_USER = "USER"
        private const val OPERAND_EVENT = "EVENT"
        private const val OPERATOR_TIMER_SPENT_IN_APP = "TIME_SPENT_IN_APP"
    }
}