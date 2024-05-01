package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.domain.controller.AppLifecycleController

internal class AppLifecycleControllerProvider(
    private val configRepository: ConfigRepositoryProvider,
    private val eventController: EventsControllerProvider,
    private val sessionHandlerProvider: RetenoSessionHandlerProvider,
    private val isLifecycleEventsEnabled: Boolean
) : ProviderWeakReference<AppLifecycleController>() {
    override fun create(): AppLifecycleController {
        return AppLifecycleController(
            configRepository = configRepository.get(),
            eventController = eventController.get(),
            isLifecycleEventTrackingEnabled = isLifecycleEventsEnabled,
            sessionHandler = sessionHandlerProvider.get()
        )
    }
}