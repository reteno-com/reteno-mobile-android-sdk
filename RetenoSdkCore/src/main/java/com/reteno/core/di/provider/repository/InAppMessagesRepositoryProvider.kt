package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.InAppMessagesRepository
import com.reteno.core.data.repository.InAppMessagesRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class InAppMessagesRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider
) : ProviderWeakReference<InAppMessagesRepository>() {

    override fun create(): InAppMessagesRepository {
        return InAppMessagesRepositoryImpl(apiClientProvider.get(), sharedPrefsManagerProvider.get())
    }
}