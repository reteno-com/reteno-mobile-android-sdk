package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.internal.action.sync.DataSync

internal class AppLifecycleControllerProvider(
    private val configRepository: ConfigRepositoryProvider,
    private val sessionHandlerProvider: RetenoSessionHandlerProvider,
    private val configProvider: RetenoConfigProvider,
    private val activityHelperProvider: RetenoActivityHelperProvider,
    private val dataSync: () -> DataSync,
    private val iamControllerProvider: IamControllerProvider
) : ProviderWeakReference<AppLifecycleController>() {
    override fun create(): AppLifecycleController {
        return AppLifecycleController(
            configRepository = configRepository.get(),
            lifecycleTrackingOptions = configProvider.get().lifecycleTrackingOptions,
            sessionHandler = sessionHandlerProvider.get(),
            activityHelper = activityHelperProvider.get(),
            clearOldData = { dataSync().clearOldData() },
            iamController = iamControllerProvider.get()
        )
    }
}