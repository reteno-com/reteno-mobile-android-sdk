package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInboxImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerAppInboxProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerAppInbox>() {

    override fun create(): RetenoDatabaseManagerAppInbox {
        return RetenoDatabaseManagerAppInboxImpl(retenoDatabaseProvider.get())
    }
}