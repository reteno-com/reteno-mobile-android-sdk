package com.reteno.core.di.provider

import com.reteno.core.data.local.cache.InteractionCacheStore
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInteractionProvider

internal class InteractionCacheProvider(
    private val retenoDatabaseManagerInteractionProvider: RetenoDatabaseManagerInteractionProvider,
) : ProviderWeakReference<InteractionCacheStore>() {

    override fun create(): InteractionCacheStore {
        return InteractionCacheStore(
            retenoDatabaseManagerInteractionProvider.get()
        )
    }
}