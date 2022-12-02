package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.RecommendationRepository
import com.reteno.core.data.repository.RecommendationRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerRecomEventsProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class RecommendationRepositoryProvider(
    private val retenoDatabaseManagerRecomEventsProvider: RetenoDatabaseManagerRecomEventsProvider,
    private val apiClientProvider: ApiClientProvider
) :
    ProviderWeakReference<RecommendationRepository>() {

    override fun create(): RecommendationRepository {
        return RecommendationRepositoryImpl(retenoDatabaseManagerRecomEventsProvider.get(), apiClientProvider.get())
    }
}