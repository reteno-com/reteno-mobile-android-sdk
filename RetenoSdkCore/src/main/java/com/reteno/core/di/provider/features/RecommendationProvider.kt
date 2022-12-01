package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.controller.RecommendationControllerProvider
import com.reteno.core.recommendation.Recommendation
import com.reteno.core.recommendation.RecommendationImpl

class RecommendationProvider(
    private val recommendationControllerProvider: RecommendationControllerProvider
) :
    ProviderWeakReference<Recommendation>() {

    override fun create(): Recommendation {
        return RecommendationImpl(recommendationControllerProvider.get())
    }
}