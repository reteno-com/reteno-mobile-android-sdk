package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLinksImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerWrappedLinkProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerWrappedLink>() {

    override fun create(): RetenoDatabaseManagerWrappedLink {
        return RetenoDatabaseManagerWrappedLinksImpl(retenoDatabaseProvider.get())
    }
}