package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerProvider(
    private val retenoDatabaseManagerDeviceProvider: RetenoDatabaseManagerDeviceProvider,
    private val retenoDatabaseManagerUserProvider: RetenoDatabaseManagerUserProvider,
    private val retenoDatabaseManagerInteractionProvider: RetenoDatabaseManagerInteractionProvider,
    private val retenoDatabaseManagerEventsProvider: RetenoDatabaseManagerEventsProvider,
    private val retenoDatabaseManagerAppInboxProvider: RetenoDatabaseManagerAppInboxProvider,
    private val retenoDatabaseManagerRecomEventsProvider: RetenoDatabaseManagerRecomEventsProvider,
    private val retenoDatabaseManagerWrappedLinkProvider: RetenoDatabaseManagerWrappedLinkProvider,
    private val retenoDatabaseManagerLogEventProvider: RetenoDatabaseManagerLogEventProvider,
    private val retenoDatabaseManagerInAppInteractionProvider: RetenoDatabaseManagerInAppInteractionProvider
    ) : ProviderWeakReference<RetenoDatabaseManager>() {

    override fun create(): RetenoDatabaseManager {
        return RetenoDatabaseManagerImpl(
            retenoDatabaseManagerDeviceProvider.get(),
            retenoDatabaseManagerUserProvider.get(),
            retenoDatabaseManagerInteractionProvider.get(),
            retenoDatabaseManagerEventsProvider.get(),
            retenoDatabaseManagerAppInboxProvider.get(),
            retenoDatabaseManagerRecomEventsProvider.get(),
            retenoDatabaseManagerWrappedLinkProvider.get(),
            retenoDatabaseManagerLogEventProvider.get(),
            retenoDatabaseManagerInAppInteractionProvider.get()
        )
    }
}