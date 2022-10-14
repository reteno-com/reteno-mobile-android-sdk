package com.reteno.di.provider

import com.reteno.di.base.ProviderWeakReference
import com.reteno.domain.controller.ContactController

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