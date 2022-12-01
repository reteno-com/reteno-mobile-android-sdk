package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEventsImpl
import com.reteno.core.di.base.ProviderWeakReference

class RetenoDatabaseManagerRecomEventsProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerRecomEvents>() {

    override fun create(): RetenoDatabaseManagerRecomEvents {
        return RetenoDatabaseManagerRecomEventsImpl(retenoDatabaseProvider.get())
    }
}