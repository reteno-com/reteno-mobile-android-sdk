package com.reteno.core.data.remote.model.iam.displayrules.schedule

import com.google.gson.JsonObject
import com.reteno.core.util.Util
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZonedDateTime

sealed class ScheduleRule {
    class ShowAfter(
        val zonedDateTime: ZonedDateTime
    ) : ScheduleRule()

    class HideAfter(
        val zonedDateTime: ZonedDateTime
    ) : ScheduleRule()

    class SpecificDaysAndTime(
        val timeZone: String,
        val daysOfWeek: List<String>,
        val fromHours: Int,
        val fromMinutes: Int,
        val toHours: Int,
        val toMinutes: Int
    ) : ScheduleRule()

    companion object {
        fun fromJson(json: JsonObject): ScheduleRule? {
            val name = json.get("name").asString
            val isActive = json.get("isActive").asBoolean

            if (name.isNullOrEmpty() || isActive.not()) return null

            return when (name) {
                null -> null
                "SHOW_AFTER" -> {
                    val params = json.getAsJsonObject("params")
                    val date = params.get("date").asString
                    val timezone = params.get("timeZone").asString
                    val scheduleTime = Util.parseWithTimeZone(date, timezone)
                    ShowAfter(scheduleTime)
                }
                "HIDE_AFTER" -> {
                    val params = json.getAsJsonObject("params")
                    val date = params.get("date").asString
                    val timezone = params.get("timeZone").asString
                    val scheduleTime = Util.parseWithTimeZone(date, timezone)
                    HideAfter(scheduleTime)
                }
                "SPECIFIC_DAYS_AND_TIME" -> {
                    val params = json.getAsJsonObject("params")
                    val timezone = params.get("timeZone").asString
                    val days = params.getAsJsonArray("days").map { it.asString }

                    val time = params.getAsJsonObject("time")
                    val from = time.getAsJsonObject("from")
                    val fromHours = from.get("hours").asInt
                    val fromMinutes = from.get("minutes").asInt

                    val to = time.getAsJsonObject("to")
                    val toHours = to.get("hours").asInt
                    val toMinutes = to.get("minutes").asInt

                    SpecificDaysAndTime(
                        timeZone = timezone,
                        daysOfWeek = days,
                        fromHours = fromHours,
                        fromMinutes = fromMinutes,
                        toHours = toHours,
                        toMinutes = toMinutes
                    )
                }
                else -> null
            }
        }
    }
}
