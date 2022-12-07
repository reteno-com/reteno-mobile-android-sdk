package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.recommendation.RecomEventsDb

interface RetenoDatabaseManagerRecomEvents {
    fun insertRecomEvents(recomEvents: RecomEventsDb)
    fun getRecomEvents(limit: Int? = null): List<RecomEventsDb>
    fun getRecomEventsCount(): Long
    fun deleteRecomEvents(count: Int, oldest: Boolean = true)
    fun deleteRecomEventsByTime(outdatedTime: String): Int
}