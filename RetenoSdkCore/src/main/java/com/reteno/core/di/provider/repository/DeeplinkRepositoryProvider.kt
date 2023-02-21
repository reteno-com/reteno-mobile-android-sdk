package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.data.repository.DeeplinkRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerWrappedLinkProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class DeeplinkRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val wrappedLinkManagerProvider: RetenoDatabaseManagerWrappedLinkProvider
) :
    ProviderWeakReference<DeeplinkRepository>() {

    override fun create(): DeeplinkRepository {
        return DeeplinkRepositoryImpl(apiClientProvider.get(), wrappedLinkManagerProvider.get())
    }
}