package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEventsImpl
import com.reteno.core.di.base.ProviderWeakReference

class RetenoDatabaseManagerEventsProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerEvents>() {

    override fun create(): RetenoDatabaseManagerEvents {
        return RetenoDatabaseManagerEventsImpl(retenoDatabaseProvider.get())
    }
}