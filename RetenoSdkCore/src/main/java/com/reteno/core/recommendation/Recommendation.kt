package com.reteno.core.recommendation

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
     * @param responseCallback Type-safe [com.reteno.core.recommendation.GetRecommendationResponseCallback]
     *
     * @see [com.reteno.core.domain.model.recommendation.get.RecomRequest]
     * @see [com.reteno.core.recommendation.GetRecommendationResponseCallback]
     */
    fun <T : RecomBase> fetchRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    )

    /**
     * 
     */
    fun logRecommendations(recomEvents: RecomEvents)
}