package com.reteno.core.di.provider

import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl
import com.reteno.core.di.base.ProviderWeakReference

class RetenoDatabaseManagerProvider :
    ProviderWeakReference<RetenoDatabaseManager>() {

    override fun create(): RetenoDatabaseManager {
        return RetenoDatabaseManagerImpl()
    }
}