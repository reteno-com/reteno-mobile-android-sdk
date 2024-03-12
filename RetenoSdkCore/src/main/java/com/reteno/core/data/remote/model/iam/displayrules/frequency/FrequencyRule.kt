package com.reteno.core.data.remote.model.iam.displayrules.frequency

import com.google.gson.JsonObject
import com.reteno.core.util.toTimeUnit
import java.util.concurrent.TimeUnit


sealed class FrequencyRule {
    object NoLimit : FrequencyRule()

    object OncePerApp : FrequencyRule()

    object OncePerSession : FrequencyRule()

    class MinInterval(
        val intervalMillis: Long
    ) : FrequencyRule()

    class TimesPerTimeUnit(
        val timeUnit: TimeUnit,
        val count: Int
    ) : FrequencyRule()

    companion object {
        fun fromJson(json: JsonObject): FrequencyRule? {
            val name = json.get("name").asString
            val isActive = json.get("isActive").asBoolean

            if (name.isNullOrEmpty() || isActive.not()) return null

            return when (name) {
                null -> null
                "NO_LIMIT" -> NoLimit
                "ONCE_PER_APP" -> OncePerApp
                "ONCE_PER_SESSION" -> OncePerSession
                "MIN_INTERVAL" -> {
                    val params = json.getAsJsonObject("params")
                    val interval = params.get("amount").asLong
                    val timeUnit = params.get("unit").asString.toTimeUnit()

                    if (timeUnit != null && interval > 0) {
                        MinInterval(timeUnit.toMillis(interval))
                    } else {
                        null
                    }
                }
                "TIMES_PER_TIME_UNIT" -> {
                    val params = json.getAsJsonObject("params")
                    val count = params.get("count").asInt
                    val timeUnit = params.get("unit").asString.toTimeUnit()

                    if (timeUnit != null && count > 0) {
                        TimesPerTimeUnit(timeUnit, count)
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
    }
}
