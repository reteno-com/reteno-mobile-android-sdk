package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter

class RuleEventValidator {

    fun checkEventMatchesRules(inapp: InAppWithEvent, event: Event): Boolean {
        return when {
            inapp.event.params.isNullOrEmpty() -> {
                true
            }

            inapp.event.paramsRelation == RuleRelation.AND -> {
                var paramsCheckSuccessful = true
                inapp.event.params.forEach { paramFromInApp ->
                    if (!checkByOperator(paramFromInApp, event)) {
                        paramsCheckSuccessful = false
                        return@forEach
                    }
                }
                paramsCheckSuccessful
            }

            inapp.event.paramsRelation == RuleRelation.OR -> {
                var paramsCheckSuccessful = false
                inapp.event.params.forEach { paramFromInApp ->
                    if (checkByOperator(paramFromInApp, event)) {
                        paramsCheckSuccessful = true
                        return@forEach
                    }
                }
                paramsCheckSuccessful
            }
            else -> false
        }
    }

    private fun checkByOperator(paramFromInApp: RuleEventParameter, event: Event): Boolean {
        return when (paramFromInApp.operator) {
            StringOperator.EQUALS -> {
                checkEquals(paramFromInApp, event.params)
            }
            StringOperator.CONTAINS -> {
                checkContains(paramFromInApp, event.params)
            }
            StringOperator.CONTAINS_ONE_OF -> {
                checkContainsOneOf(paramFromInApp, event.params)
            }
            StringOperator.STARTS_WITH -> {
                checkStartsWith(paramFromInApp, event.params)
            }
            StringOperator.ENDS_WITH -> {
                checkEndsWith(paramFromInApp, event.params)
            }
            StringOperator.REG_EX -> {
                checkRegex(paramFromInApp, event.params)
            }
        }
    }

    private fun checkEquals(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        val paramFromEvent = eventParams?.firstOrNull {
            it.name == paramFromInApp.name
        } ?: return false

        var equalsOneOfValues = false
        paramFromInApp.values.forEach {
            if (paramFromEvent.value == it) {
                equalsOneOfValues = true
                return@forEach
            }
        }

        return equalsOneOfValues
    }

    private fun checkContains(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        val paramFromEvent = eventParams?.firstOrNull {
            it.name == paramFromInApp.name
        } ?: return false

        paramFromInApp.values.forEach{
            if (paramFromEvent.value?.contains(it) == true) {
                return true
            }
        }
        return false
    }

    private fun checkContainsOneOf(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        if (eventParams.isNullOrEmpty()) {
            return false
        }

        val eventParamNames = eventParams.map { it.name }
        var containsAtLeastOneParam = false
        paramFromInApp.values.forEach{
            if (eventParamNames.contains(it)) {
                containsAtLeastOneParam = true
                return@forEach
            }
        }

        return containsAtLeastOneParam
    }

    private fun checkStartsWith(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        val paramFromEvent = eventParams?.firstOrNull {
            it.name == paramFromInApp.name
        }

        if (paramFromEvent?.value == null) {
            return false
        }

        var startsWithOneOfValues = false
        paramFromInApp.values.forEach{
            if (paramFromEvent.value.startsWith(it, ignoreCase = true)) {
                startsWithOneOfValues = true
                return@forEach
            }
        }

        return startsWithOneOfValues
    }

    private fun checkEndsWith(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        val paramFromEvent = eventParams?.firstOrNull {
            it.name == paramFromInApp.name
        }

        if (paramFromEvent?.value == null) {
            return false
        }

        var endsWithOneOfValues = false
        paramFromInApp.values.forEach{
            if (paramFromEvent.value.endsWith(it, ignoreCase = true)) {
                endsWithOneOfValues = true
                return@forEach
            }
        }

        return endsWithOneOfValues
    }

    private fun checkRegex(paramFromInApp: RuleEventParameter, eventParams: List<Parameter>?): Boolean {
        val paramFromEvent = eventParams?.firstOrNull {
            it.name == paramFromInApp.name
        }
        if (paramFromEvent?.value == null) {
            return false
        }

        var matchesOneOfValues = false
        paramFromInApp.values.forEach inner@{
            if (Regex(it).matches(paramFromEvent.value)) {
                matchesOneOfValues = true
                return@inner
            }
        }

        return matchesOneOfValues
    }
}