package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerUserProvider
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.repository.IamRepositoryProvider
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.IamControllerImpl

internal class IamControllerProvider(
    private val iamRepositoryProvider: IamRepositoryProvider,
    private val sessionHandlerProvider: RetenoSessionHandlerProvider,
    private val eventsControllerProvider: EventsControllerProvider,
    private val userDbProvider: RetenoDatabaseManagerUserProvider
) : ProviderWeakReference<IamController>() {

    override fun create(): IamController {
        return IamControllerImpl(
            iamRepositoryProvider.get(),
            eventsControllerProvider.get(),
            sessionHandlerProvider.get(),
            userDbProvider.get()
        )
    }
}