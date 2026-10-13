package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.data.repository.DeeplinkRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerWrappedLinkProvider
import com.reteno.core.internal.action.sync.DataSync

internal class DeeplinkRepositoryProvider(
    private val wrappedLinkManagerProvider: RetenoDatabaseManagerWrappedLinkProvider,
    private val dataSync: () -> DataSync
) :
    ProviderWeakReference<DeeplinkRepository>() {

    override fun create(): DeeplinkRepository {
        return DeeplinkRepositoryImpl(wrappedLinkManagerProvider.get(), requestSync = { dataSync().request() })
    }
}