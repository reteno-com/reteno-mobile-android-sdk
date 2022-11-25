package com.reteno.core.recommendation

import com.reteno.core.domain.controller.RecommendationController
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest

class RecommendationImpl(private val recommendationController: RecommendationController) :
    Recommendation {

    override fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        recommendationController.getRecommendation(
            recomVariantId,
            recomRequest,
            responseClass,
            responseCallback
        )
    }
}