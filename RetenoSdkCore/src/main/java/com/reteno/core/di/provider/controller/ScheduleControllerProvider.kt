package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.WorkManagerProvider
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.controller.ScheduleControllerImpl

internal class ScheduleControllerProvider(
    private val contactControllerProvider: ContactControllerProvider,
    private val interactionControllerProvider: InteractionControllerProvider,
    private val eventsControllerProvider: EventsControllerProvider,
    private val appInboxControllerProvider: AppInboxControllerProvider,
    private val recommendationControllerProvider: RecommendationControllerProvider,
    private val deeplinkControllerProvider: DeeplinkControllerProvider,
    private val workManagerProvider: WorkManagerProvider
) : ProviderWeakReference<ScheduleController>() {

    override fun create(): ScheduleController {
        return ScheduleControllerImpl(
            contactControllerProvider.get(),
            interactionControllerProvider.get(),
            eventsControllerProvider.get(),
            appInboxControllerProvider.get(),
            recommendationControllerProvider.get(),
            deeplinkControllerProvider.get(),
            workManagerProvider
        )
    }
}