package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.domain.controller.DeeplinkController

class DeeplinkControllerProvider(
    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider
) :
    ProviderWeakReference<DeeplinkController>() {

    override fun create(): DeeplinkController {
        return DeeplinkController(deeplinkRepositoryProvider.get())
    }
}