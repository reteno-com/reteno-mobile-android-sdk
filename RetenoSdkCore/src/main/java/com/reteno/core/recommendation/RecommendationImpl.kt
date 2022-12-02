package com.reteno.core.recommendation

import com.reteno.core.RetenoImpl
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
        try {
            recommendationController.getRecommendation(
                recomVariantId,
                recomRequest,
                responseClass,
                responseCallback
            )
        } catch (ex: Throwable) {
            Logger.e(TAG, "fetchRecommendation(): ", ex)
        }
    }

    override fun logRecommendations(recomEvents: RecomEvents) {
        /*@formatter:off*/ Logger.i(TAG, "logRecommendations(): ", "recomEvents = [" , recomEvents , "]")
        /*@formatter:on*/
        try {
            recommendationController.trackRecommendations(recomEvents)
        } catch (ex: Throwable) {
            Logger.e(TAG, "logRecommendations(): ", ex)
        }
    }

    companion object {
        val TAG: String = RecommendationImpl::class.java.simpleName
    }
}