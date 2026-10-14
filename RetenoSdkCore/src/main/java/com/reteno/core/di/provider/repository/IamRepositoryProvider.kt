package com.reteno.core.di.provider.repository

import android.content.Context
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.data.repository.IamRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppMessagesProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import kotlinx.coroutines.CoroutineDispatcher

internal class IamRepositoryProvider(
    private val context: Context,
    private val fileManagerProvider: FileManagerProvider,
    private val apiClientProvider: ApiClientProvider,
    private val retenoDatabaseManagerInAppMessagesProvider: RetenoDatabaseManagerInAppMessagesProvider,
    private val coroutineDispatcher: CoroutineDispatcher,
) : ProviderWeakReference<IamRepository>() {

    override fun create(): IamRepository {
        return IamRepositoryImpl(
            context,
            apiClientProvider.get(),
            fileManagerProvider.get(),
            retenoDatabaseManagerInAppMessagesProvider.get(),
            coroutineDispatcher
        )
    }
}