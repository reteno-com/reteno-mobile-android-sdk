package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
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