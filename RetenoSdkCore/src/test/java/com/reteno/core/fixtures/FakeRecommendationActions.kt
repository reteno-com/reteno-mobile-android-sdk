package com.reteno.core.fixtures

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.recommendation.RecommendationActions

internal class FakeRecommendationActions(private val log: FakeCallLog = FakeCallLog()) : RecommendationActions {
    val callbacks = mutableListOf<Any>()
    val recomEvents = mutableListOf<RecomEvents>()

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        log.record("submitRecommendationRequest $recomVariantId ${responseClass.simpleName} $requestBody")
        callbacks += callback
    }

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) {
        log.record("submitRecommendationJsonRequest $recomVariantId $requestBody")
        callbacks += callback
    }

    override fun submitRecommendationEvents(events: RecomEvents) {
        log.record("submitRecommendationEvents ${events.recomVariantId}")
        recomEvents += events
    }
}
