package com.reteno.core.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.controller.RecommendationController
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.util.Logger

class RecommendationImpl(private val recommendationController: RecommendationController) :
    Recommendation {

    override fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "fetchRecommendation(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseClass = [" , responseClass , "], responseCallback = [" , responseCallback , "]")
        /*@formatter:on*/
        recommendationController.getRecommendation(
            recomVariantId,
            recomRequest,
            responseClass,
            responseCallback
        )
    }

    override fun logRecommendations(recomEvents: RecomEvents) {
        /*@formatter:off*/ Logger.i(TAG, "logRecommendations(): ", "recomEvents = [" , recomEvents , "]")
        /*@formatter:on*/
        recommendationController.trackRecommendations(recomEvents)
    }

    companion object {
        val TAG: String = RecommendationImpl::class.java.simpleName
    }
}