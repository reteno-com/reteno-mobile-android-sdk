package com.reteno.core.features.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents

interface Recommendation {

    /**
     * Obtain Recommendations.
     *
     * @param recomVariantId recommendation variant id. It is in r{recomId}v{variantId} format. Where recomId and variant Id are integer identifiers
     * @param recomRequest [com.reteno.core.domain.model.recommendation.get.RecomRequest] model
     * @param responseClass Class type to cast response to
     * @param responseCallback Type-safe [com.reteno.core.features.recommendation.GetRecommendationResponseCallback]
     *
     * @see [com.reteno.core.domain.model.recommendation.get.RecomRequest]
     * @see [com.reteno.core.features.recommendation.GetRecommendationResponseCallback]
     */
    fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    )

    /**
     * Obtain Recommendations JSON.
     *
     * @param recomVariantId recommendation variant id. It is in r{recomId}v{variantId} format. Where recomId and variant Id are integer identifiers
     * @param recomRequest [com.reteno.core.domain.model.recommendation.get.RecomRequest] model
     * @param responseCallback [com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback]
     *
     * @see [com.reteno.core.domain.model.recommendation.get.RecomRequest]
     * @see [com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback]
     */
    fun fetchRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    )

    /**
     * Log recommendation events
     * @param recomEvents - recommendation event to be logged
     *
     * @see [com.reteno.core.domain.model.recommendation.post.RecomEvents]
     */
    fun logRecommendations(recomEvents: RecomEvents)
}