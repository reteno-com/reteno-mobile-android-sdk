package com.reteno.core.data.repository

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import java.time.ZonedDateTime

internal interface RecommendationRepository {

    fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    )

    fun saveRecommendations(recomEvents: RecomEvents)
    fun pushRecommendations()
    fun clearOldRecommendations(outdatedTime: ZonedDateTime)
}