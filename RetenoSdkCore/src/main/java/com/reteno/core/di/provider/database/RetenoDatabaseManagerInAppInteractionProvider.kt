package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteractionImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerInAppInteractionProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerInAppInteraction>() {

    override fun create(): RetenoDatabaseManagerInAppInteraction {
        return RetenoDatabaseManagerInAppInteractionImpl(retenoDatabaseProvider.get())
    }
}