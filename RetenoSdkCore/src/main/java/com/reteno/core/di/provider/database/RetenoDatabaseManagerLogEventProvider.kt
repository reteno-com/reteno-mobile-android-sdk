package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEventImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerLogEventProvider(
    private val retenoDatabaseProvider: DatabaseProvider
) : ProviderWeakReference<RetenoDatabaseManagerLogEvent>() {

    override fun create(): RetenoDatabaseManagerLogEvent {
        return RetenoDatabaseManagerLogEventImpl(retenoDatabaseProvider.get())
    }
}