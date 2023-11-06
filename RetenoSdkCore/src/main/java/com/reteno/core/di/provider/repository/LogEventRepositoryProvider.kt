package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.LogEventRepository
import com.reteno.core.data.repository.LogEventRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerLogEventProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class LogEventRepositoryProvider(
    private val retenoDatabaseManagerLogEventProvider: RetenoDatabaseManagerLogEventProvider,
    private val apiClientProvider: ApiClientProvider
) :
    ProviderWeakReference<LogEventRepository>() {

    override fun create(): LogEventRepository {
        return LogEventRepositoryImpl(retenoDatabaseManagerLogEventProvider.get(), apiClientProvider.get())
    }
}