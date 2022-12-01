package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.data.repository.ContactRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerDeviceProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerUserProvider
import com.reteno.core.di.provider.network.ApiClientProvider

class ContactRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider,
    private val retenoDatabaseManagerDeviceProvider: RetenoDatabaseManagerDeviceProvider,
    private val retenoDatabaseManagerUserProvider: RetenoDatabaseManagerUserProvider
) : ProviderWeakReference<ContactRepository>() {

    override fun create(): ContactRepository {
        return ContactRepositoryImpl(
            apiClientProvider.get(),
            configRepositoryProvider.get(),
            retenoDatabaseManagerDeviceProvider.get(),
            retenoDatabaseManagerUserProvider.get()
        )
    }
}