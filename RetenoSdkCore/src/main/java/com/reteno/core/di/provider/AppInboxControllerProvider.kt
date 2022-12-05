package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.domain.controller.AppInboxController

class AppInboxControllerProvider(
    private val appInboxRepositoryProvider: AppInboxRepositoryProvider
) : ProviderWeakReference<AppInboxController>() {

    override fun create(): AppInboxController {
        return AppInboxController(
            appInboxRepositoryProvider.get()
        )
    }
}