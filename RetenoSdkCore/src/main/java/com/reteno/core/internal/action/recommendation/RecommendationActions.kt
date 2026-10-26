package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback

internal interface RecommendationActions {
    fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    )

    fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    )

    fun submitRecommendationEvents(events: RecomEvents)
}
