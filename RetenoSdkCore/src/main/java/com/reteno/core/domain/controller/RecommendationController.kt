package com.reteno.core.domain.controller

import com.reteno.core.data.repository.RecommendationRepository
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.recommendation.GetRecommendationResponseCallback

class RecommendationController(private val recommendationRepository: RecommendationRepository) {

    fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        recommendationRepository.getRecommendation(
            recomVariantId,
            recomRequest,
            responseClass,
            responseCallback
        )
    }
}