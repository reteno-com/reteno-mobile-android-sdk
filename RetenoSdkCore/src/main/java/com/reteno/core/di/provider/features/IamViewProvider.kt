package com.reteno.core.di.provider.features

import com.reteno.core.di.base.Provider
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.controller.IamControllerProvider
import com.reteno.core.di.provider.controller.InteractionControllerProvider
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.view.iam.IamView
import com.reteno.core.view.iam.IamViewImpl

internal class IamViewProvider(
    private val retenoActivityHelperProvider: RetenoActivityHelperProvider,
    private val iamControllerProvider: IamControllerProvider,
    private val interactionControllerProvider: InteractionControllerProvider,
    private val scheduleControllerProvider: Provider<ScheduleController>
) : ProviderWeakReference<IamView>() {

    override fun create(): IamView {
        return IamViewImpl(
            retenoActivityHelperProvider.get(),
            iamControllerProvider.get(),
            interactionControllerProvider.get(),
            scheduleControllerProvider.get()
        )
    }
}