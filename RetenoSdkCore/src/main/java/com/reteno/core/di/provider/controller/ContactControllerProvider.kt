package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.di.provider.repository.ContactRepositoryProvider
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.internal.action.sync.DataSync
import com.reteno.core.internal.action.sync.SyncScope

internal class ContactControllerProvider(
    private val contactRepositoryProvider: ContactRepositoryProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider,
    private val dataSync: () -> DataSync
) : ProviderWeakReference<ContactController>() {

    override fun create(): ContactController {
        return ContactController(
            contactRepositoryProvider.get(),
            configRepositoryProvider.get(),
            syncContactNow = { dataSync().syncNow(SyncScope.CONTACT) }
        )
    }
}