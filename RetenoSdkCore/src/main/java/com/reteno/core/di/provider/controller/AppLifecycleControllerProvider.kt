package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.Provider
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.domain.controller.ScheduleController

internal class AppLifecycleControllerProvider(
    private val configRepository: ConfigRepositoryProvider,
    private val eventController: EventsControllerProvider,
    private val sessionHandlerProvider: RetenoSessionHandlerProvider,
    private val configProvider: RetenoConfigProvider,
    private val activityHelperProvider: RetenoActivityHelperProvider,
    private val scheduleControllerProvider: Provider<ScheduleController>,
    private val iamControllerProvider: IamControllerProvider
) : ProviderWeakReference<AppLifecycleController>() {
    override fun create(): AppLifecycleController {
        return AppLifecycleController(
            configRepository = configRepository.get(),
            eventController = eventController.get(),
            lifecycleTrackingOptions = configProvider.get().lifecycleTrackingOptions,
            sessionHandler = sessionHandlerProvider.get(),
            activityHelper = activityHelperProvider.get(),
            scheduleController = scheduleControllerProvider.get(),
            iamController = iamControllerProvider.get()
        )
    }
}