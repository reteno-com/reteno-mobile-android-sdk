package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.domain.controller.ScreenTrackingController

internal class ScreenTrackingControllerProvider(
    private val retenoActivityHelperProvider: RetenoActivityHelperProvider,
    private val eventsControllerProvider: EventsControllerProvider
) :
    ProviderWeakReference<ScreenTrackingController>() {

    override fun create(): ScreenTrackingController {
        return ScreenTrackingController(
            retenoActivityHelperProvider.get(),
            eventsControllerProvider.get()
        )
    }
}