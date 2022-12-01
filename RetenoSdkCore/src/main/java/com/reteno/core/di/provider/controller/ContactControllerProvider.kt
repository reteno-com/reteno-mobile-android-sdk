package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.di.provider.repository.ContactRepositoryProvider
import com.reteno.core.domain.controller.ContactController

class ContactControllerProvider(
    private val contactRepositoryProvider: ContactRepositoryProvider,
    private val configRepositoryProvider: ConfigRepositoryProvider
) :
    ProviderWeakReference<ContactController>() {

    override fun create(): ContactController {
        return ContactController(
            contactRepositoryProvider.get(),
            configRepositoryProvider.get()
        )
    }
}