package com.reteno.core.di.provider.repository

import com.reteno.core.data.repository.IamRepository
import com.reteno.core.data.repository.IamRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.network.ApiClientProvider

internal class IamRepositoryProvider(
    private val apiClientProvider: ApiClientProvider,
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider
) : ProviderWeakReference<IamRepository>() {

    override fun create(): IamRepository {
        return IamRepositoryImpl(apiClientProvider.get(), sharedPrefsManagerProvider.get())
    }
}