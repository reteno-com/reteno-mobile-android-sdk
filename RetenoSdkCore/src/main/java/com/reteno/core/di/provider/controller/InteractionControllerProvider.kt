package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.di.provider.repository.InteractionRepositoryProvider
import com.reteno.core.domain.controller.InteractionController

internal class InteractionControllerProvider(
    private val configRepositoryProvider: ConfigRepositoryProvider,
    private val interactionRepositoryProvider: InteractionRepositoryProvider
) :
    ProviderWeakReference<InteractionController>() {

    override fun create(): InteractionController {
        return InteractionController(
            configRepositoryProvider.get(),
            interactionRepositoryProvider.get()
        )
    }
}