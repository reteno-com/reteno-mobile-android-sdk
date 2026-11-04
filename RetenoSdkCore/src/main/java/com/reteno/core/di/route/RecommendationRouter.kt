package com.reteno.core.di.route

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.di.graph.RecommendationGraph
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.recommendation.RecommendationActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class RecommendationRouter(
    private val recommendation: RecommendationGraph,
    private val scope: CoroutineScope
) : RecommendationActions {

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        scope.launch { recommendation.fetchRecommendation(recomVariantId, requestBody, responseClass, callback) }
    }

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) {
        scope.launch { recommendation.fetchRecommendationJson(recomVariantId, requestBody, callback) }
    }

    override fun submitRecommendationEvents(events: RecomEvents) {
        recommendation.eventsQueue.enqueue { recommendation.logRecommendationEvents(events) }
    }
}
