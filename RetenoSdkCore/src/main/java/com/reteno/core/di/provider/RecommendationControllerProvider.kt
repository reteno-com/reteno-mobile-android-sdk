package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.domain.controller.RecommendationController

class RecommendationControllerProvider(
    private val recommendationRepositoryProvider: RecommendationRepositoryProvider
) :
    ProviderWeakReference<RecommendationController>() {

    override fun create(): RecommendationController {
        return RecommendationController(recommendationRepositoryProvider.get())
    }
}