package com.reteno.core.data.remote.model.iam.displayrules.frequency

enum class FrequencyRuleType {
    NO_LIMIT, ONCE_PER_APP, ONCE_PER_SESSION, MIN_INTERVAL, TIMES_PER_TIME_UNIT;

    companion object {
        fun fromString(name: String?): FrequencyRuleType? {
            return when (name) {
                "NO_LIMIT" -> NO_LIMIT
                "ONCE_PER_APP" -> ONCE_PER_APP
                "ONCE_PER_SESSION" -> ONCE_PER_SESSION
                "MIN_INTERVAL" -> MIN_INTERVAL
                "TIMES_PER_TIME_UNIT" -> TIMES_PER_TIME_UNIT
                else -> null
            }
        }
    }
}