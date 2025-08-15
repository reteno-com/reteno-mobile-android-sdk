package com.reteno.core.data.repository

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import java.time.ZonedDateTime

internal interface RecommendationRepository {

    fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    )
    fun getRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    )

    fun saveRecommendations(recomEvents: RecomEvents)
    fun pushRecommendations()
    fun clearOldRecommendations(outdatedTime: ZonedDateTime)
}