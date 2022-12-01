package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUserImpl
import com.reteno.core.di.base.ProviderWeakReference

class RetenoDatabaseManagerUserProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerUser>() {

    override fun create(): RetenoDatabaseManagerUser {
        return RetenoDatabaseManagerUserImpl(retenoDatabaseProvider.get())
    }
}