package com.reteno.core.features.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.controller.RecommendationController
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported

internal class RecommendationImpl(private val recommendationController: RecommendationController) :
    Recommendation {

    override fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        if (!isOsVersionSupported()) {
            return
        }
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

    override fun fetchRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    ) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "fetchRecommendationJson(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseCallback = [" , responseCallback , "]")
        /*@formatter:on*/
        try {
            recommendationController.getRecommendationJson(
                recomVariantId,
                recomRequest,
                responseCallback
            )
        } catch (ex: Throwable) {
            Logger.e(TAG, "fetchRecommendationJson(): ", ex)
        }
    }

    override fun logRecommendations(recomEvents: RecomEvents) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "logRecommendations(): ", "recomEvents = [" , recomEvents , "]")
        /*@formatter:on*/
        try {
            recommendationController.trackRecommendations(recomEvents)
        } catch (ex: Throwable) {
            Logger.e(TAG, "logRecommendations(): ", ex)
        }
    }

    companion object {
        private val TAG: String = RecommendationImpl::class.java.simpleName
    }
}