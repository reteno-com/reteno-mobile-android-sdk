package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessagesImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerInAppMessagesProvider(
    private val retenoDatabaseProvider: DatabaseProvider
) : ProviderWeakReference<RetenoDatabaseManagerInAppMessages>() {

    override fun create(): RetenoDatabaseManagerInAppMessages {
        return RetenoDatabaseManagerInAppMessagesImpl(retenoDatabaseProvider.get())
    }
}