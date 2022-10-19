package com.reteno.core.di.provider

import com.reteno.core.data.remote.ds.InteractionRepository
import com.reteno.core.data.remote.ds.InteractionRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class InteractionRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<InteractionRepository>() {

    override fun create(): InteractionRepository {
        return InteractionRepositoryImpl(apiClientProvider.get())
    }
}