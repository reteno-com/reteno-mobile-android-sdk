package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.DeeplinkRepositoryProvider
import com.reteno.core.domain.controller.DeeplinkController

internal class DeeplinkControllerProvider(
    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider
) :
    ProviderWeakReference<DeeplinkController>() {

    override fun create(): DeeplinkController {
        return DeeplinkController(deeplinkRepositoryProvider.get())
    }
}