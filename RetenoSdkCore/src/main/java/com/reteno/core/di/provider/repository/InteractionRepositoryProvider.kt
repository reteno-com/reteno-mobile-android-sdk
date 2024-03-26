package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.data.repository.InteractionRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppInteractionProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInteractionProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class InteractionRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val retenoDatabaseManagerInteractionProvider: RetenoDatabaseManagerInteractionProvider,
    private val retenoDatabaseManagerInAppInteractionProvider: RetenoDatabaseManagerInAppInteractionProvider
) : ProviderWeakReference<InteractionRepository>() {

    override fun create(): InteractionRepository {
        return InteractionRepositoryImpl(
            apiClientProvider.get(),
            retenoDatabaseManagerInteractionProvider.get(),
            retenoDatabaseManagerInAppInteractionProvider.get()
        )
    }
}