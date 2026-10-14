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
    private val actions: RecommendationActions,
    private val remoteLog: RemoteLog,
    private val isOsSupported: () -> Boolean
) : Recommendation {

    override fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        passToSdk("fetchRecommendation") {
            val requestBody = recomRequest.toRemote().toJson()
            actions.submitRecommendationRequest(recomVariantId, requestBody, responseClass, responseCallback)
        }
    }

    override fun fetchRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    ) {
        passToSdk("fetchRecommendationJson") {
            val requestBody = recomRequest.toRemote().toJson()
            actions.submitRecommendationJsonRequest(recomVariantId, requestBody, responseCallback)
        }
    }

    override fun logRecommendations(recomEvents: RecomEvents) {
        passToSdk("logRecommendations") {
            actions.submitRecommendationEvents(recomEvents)
        }
    }

    private fun passToSdk(call: String, submit: () -> Unit) {
        if (!isOsSupported()) return
        try {
            submit()
        } catch (error: Throwable) {
            remoteLog.error("Recommendation.$call failed: ${Redact.summary(error)}")
        }
    }
}
