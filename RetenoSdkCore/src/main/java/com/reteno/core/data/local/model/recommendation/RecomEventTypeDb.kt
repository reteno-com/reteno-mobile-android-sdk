package com.reteno.core.data.local.model.recommendation;

enum class RecomEventTypeDb {
    IMPRESSIONS, CLICKS;

    companion object {
        fun fromString(value: String?): RecomEventTypeDb? =
            when (value) {
                IMPRESSIONS.toString() -> IMPRESSIONS
                CLICKS.toString() -> CLICKS
                else -> null
            }
    }
}