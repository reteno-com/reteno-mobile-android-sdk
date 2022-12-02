package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteractionImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerInteractionProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerInteraction>() {

    override fun create(): RetenoDatabaseManagerInteraction {
        return RetenoDatabaseManagerInteractionImpl(retenoDatabaseProvider.get())
    }
}