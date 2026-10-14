package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.database.RetenoDatabaseManagerUserProvider
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.repository.IamRepositoryProvider
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.IamControllerImpl
import com.reteno.core.internal.action.iam.CheckInAppSegments
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState

internal class IamControllerProvider(
    private val iamRepositoryProvider: IamRepositoryProvider,
    private val sessionHandlerProvider: RetenoSessionHandlerProvider,
    private val userDbProvider: RetenoDatabaseManagerUserProvider,
    private val displayRules: () -> InAppDisplayRules,
    private val state: () -> InAppMessagesState,
    private val checkSegments: () -> CheckInAppSegments
) : ProviderWeakReference<IamController>() {

    override fun create(): IamController {
        return IamControllerImpl(
            iamRepositoryProvider.get(),
            sessionHandlerProvider.get(),
            userDbProvider.get(),
            displayRules(),
            state(),
            checkSegments()
        )
    }
}
