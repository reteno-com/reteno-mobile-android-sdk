package com.reteno.core.di.provider

import com.reteno.core.data.repository.DeeplinkRepositoryImpl
import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.di.base.ProviderWeakReference

class DeeplinkRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<DeeplinkRepository>() {

    override fun create(): DeeplinkRepository {
        return DeeplinkRepositoryImpl(apiClientProvider.get())
    }
}