package com.reteno.core.domain.model.recommendation.post

enum class RecomEventType {
    IMPRESSIONS, CLICKS;

    companion object {
        fun fromString(value: String?): RecomEventType? =
            when (value) {
                IMPRESSIONS.toString() -> IMPRESSIONS
                CLICKS.toString() -> CLICKS
                else -> null
            }
    }
}