package com.reteno.core.di.provider

import com.reteno.core.data.repository.RecommendationRepository
import com.reteno.core.data.repository.RecommendationRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class RecommendationRepositoryProvider(
    private val databaseManagerProvider: DatabaseManagerProvider,
    private val apiClientProvider: ApiClientProvider
) :
    ProviderWeakReference<RecommendationRepository>() {

    override fun create(): RecommendationRepository {
        return RecommendationRepositoryImpl(databaseManagerProvider.get(), apiClientProvider.get())
    }
}