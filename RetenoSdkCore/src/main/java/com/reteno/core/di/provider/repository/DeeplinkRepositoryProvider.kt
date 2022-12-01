package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.data.repository.DeeplinkRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.network.ApiClientProvider

class DeeplinkRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<DeeplinkRepository>() {

    override fun create(): DeeplinkRepository {
        return DeeplinkRepositoryImpl(apiClientProvider.get())
    }
}