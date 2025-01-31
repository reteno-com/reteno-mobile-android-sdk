package com.reteno.core.di.provider.repository

import android.content.Context
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.data.repository.IamRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppMessagesProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import kotlinx.coroutines.CoroutineDispatcher

internal class IamRepositoryProvider(
    private val context: Context,
    private val apiClientProvider: ApiClientProvider,
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val retenoDatabaseManagerInAppMessagesProvider: RetenoDatabaseManagerInAppMessagesProvider,
    private val coroutineDispatcher: CoroutineDispatcher,
) : ProviderWeakReference<IamRepository>() {

    override fun create(): IamRepository {
        return IamRepositoryImpl(
            context,
            apiClientProvider.get(),
            sharedPrefsManagerProvider.get(),
            retenoDatabaseManagerInAppMessagesProvider.get(),
            coroutineDispatcher
        )
    }
}