package com.reteno.core.di.provider

import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl
import com.reteno.core.di.base.ProviderWeakReference

class DatabaseManagerProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManager>() {

    override fun create(): RetenoDatabaseManager {
        return RetenoDatabaseManagerImpl(retenoDatabaseProvider.get())
    }
}