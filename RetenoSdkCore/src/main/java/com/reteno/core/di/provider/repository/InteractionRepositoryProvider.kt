package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.data.repository.InteractionRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppInteractionProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInteractionProvider
import com.reteno.core.internal.action.sync.DataSync

internal class InteractionRepositoryProvider(
    private val retenoDatabaseManagerInteractionProvider: RetenoDatabaseManagerInteractionProvider,
    private val retenoDatabaseManagerInAppInteractionProvider: RetenoDatabaseManagerInAppInteractionProvider,
    private val dataSync: () -> DataSync
) : ProviderWeakReference<InteractionRepository>() {

    override fun create(): InteractionRepository {
        return InteractionRepositoryImpl(
            retenoDatabaseManagerInteractionProvider.get(),
            retenoDatabaseManagerInAppInteractionProvider.get(),
            requestSync = { dataSync().request() }
        )
    }
}