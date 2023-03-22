package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.controller.RecommendationControllerProvider
import com.reteno.core.features.recommendation.Recommendation
import com.reteno.core.features.recommendation.RecommendationImpl

internal class RecommendationProvider(
    private val recommendationControllerProvider: RecommendationControllerProvider
) :
    ProviderWeakReference<Recommendation>() {

    override fun create(): Recommendation {
        return RecommendationImpl(recommendationControllerProvider.get())
    }
}