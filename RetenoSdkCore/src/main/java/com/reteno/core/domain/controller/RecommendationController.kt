package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.repository.RecommendationRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.util.Logger

internal class RecommendationController(private val recommendationRepository: RecommendationRepository) {

    fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getRecommendation(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseClass = [" , responseClass , "], responseCallback = [" , responseCallback , "]")
        /*@formatter:on*/
        recommendationRepository.getRecommendation(
            recomVariantId,
            recomRequest,
            responseClass,
            responseCallback
        )
    }

    fun getRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getRecommendationJson(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseCallback = [" , responseCallback , "]")
        /*@formatter:on*/
        recommendationRepository.getRecommendationJson(
            recomVariantId,
            recomRequest,
            responseCallback
        )
    }

    fun trackRecommendations(recomEvents: RecomEvents) {
        /*@formatter:off*/ Logger.i(TAG, "trackRecommendations(): ", "recomEvents = [" , recomEvents , "]")
        /*@formatter:on*/
        recommendationRepository.saveRecommendations(recomEvents)
    }

    internal fun pushRecommendations() {
        /*@formatter:off*/ Logger.i(TAG, "pushRecommendations(): ", "")
        /*@formatter:on*/
        recommendationRepository.pushRecommendations()
    }

    internal fun clearOldRecommendations() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldRecommendations(): ", "")
        /*@formatter:on*/
        val outdatedTime = SchedulerUtils.getOutdatedTime()
        recommendationRepository.clearOldRecommendations(outdatedTime)
    }

    companion object {
        private val TAG: String = RecommendationController::class.java.simpleName
    }
}