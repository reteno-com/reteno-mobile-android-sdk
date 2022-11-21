package com.reteno.core.di.provider

import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.data.repository.InteractionRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class InteractionRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val databaseManagerProvider: DatabaseManagerProvider
) : ProviderWeakReference<InteractionRepository>() {

    override fun create(): InteractionRepository {
        return InteractionRepositoryImpl(
            apiClientProvider.get(),
            databaseManagerProvider.get()
        )
    }
}