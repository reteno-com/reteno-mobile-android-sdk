package com.reteno.core.data.remote.model.iam.displayrules.frequency

import com.google.gson.JsonObject
import java.util.concurrent.TimeUnit


sealed class FrequencyRule() {
    object NoLimit : FrequencyRule()

    object OncePerApp : FrequencyRule()

    object OncePerSession : FrequencyRule()

    class MinInterval(
        val timeUnit: TimeUnit,
        val amount: Long
    ) : FrequencyRule()

    class TimesPerTimeUnit(
        val timeUnit: TimeUnit,
        val count: Long
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
                    // parse params model
                    //"type": "FREQUENCY",
                    //          "predicates": [
                    //            {
                    //              "name": "MIN_INTERVAL",
                    //              "isActive": true,
                    //              "params": {
                    //                "unit": "HOUR",
                    //                "amount": 1
                    //              }
                    //            }
                    //          ]
//                    val interval = json.get("amount").asLong
//                    val timeUnit = json.get("timeUnit").asString.toTimeUnit()
//
//                    if (timeUnit != null) {
//                        MinInterval(timeUnit, interval)
//                    } else {
//                        null
//                    }
                    null
                }
                "TIMES_PER_TIME_UNIT" -> {
//                    val count = json.get("count").asLong
//                    val timeUnit = json.get("timeUnit").asString.toTimeUnit()
//
//                    if (timeUnit != null) {
//                        TimesPerTimeUnit(timeUnit, count)
//                    } else {
//                        null
//                    }
                    null
                }
                else -> null
            }
        }
    }
}
