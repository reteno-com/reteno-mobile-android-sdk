package com.reteno.core.features.recommendation

import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.internal.action.recommendation.RecommendationActions
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog

internal class RecommendationImpl(
    private val recommendationActions: RecommendationActions,
    private val remoteLog: RemoteLog
) : Recommendation {

    override fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        passToSdk("fetchRecommendation") {
            val requestBody = recomRequest.toRemote().toJson()
            recommendationActions.submitRecommendationRequest(
                recomVariantId,
                requestBody,
                responseClass,
                responseCallback
            )
        }
    }

    override fun fetchRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    ) {
        passToSdk("fetchRecommendationJson") {
            val requestBody = recomRequest.toRemote().toJson()
            recommendationActions.submitRecommendationJsonRequest(recomVariantId, requestBody, responseCallback)
        }
    }

    override fun logRecommendations(recomEvents: RecomEvents) {
        passToSdk("logRecommendations") {
            recommendationActions.submitRecommendationEvents(recomEvents)
        }
    }

    private fun passToSdk(call: String, submit: () -> Unit) {
        try {
            submit()
        } catch (error: Throwable) {
            remoteLog.error("Recommendation.$call failed: ${Redact.summary(error)}")
        }
    }
}
