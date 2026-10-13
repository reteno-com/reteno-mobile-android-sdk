package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.controller.IamControllerProvider
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.core.view.iam.IamView
import com.reteno.core.view.iam.IamViewImpl

internal class IamViewProvider(
    private val retenoActivityHelperProvider: RetenoActivityHelperProvider,
    private val iamControllerProvider: IamControllerProvider,
    private val interactions: () -> InteractionSubmitter
) : ProviderWeakReference<IamView>() {

    override fun create(): IamView {
        return IamViewImpl(
            retenoActivityHelperProvider.get(),
            iamControllerProvider.get(),
            interactions()
        )
    }
}
