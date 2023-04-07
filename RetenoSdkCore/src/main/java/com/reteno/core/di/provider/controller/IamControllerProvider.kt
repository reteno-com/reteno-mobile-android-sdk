package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.IamRepositoryProvider
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.IamControllerImpl

internal class IamControllerProvider(
    private val iamRepositoryProvider: IamRepositoryProvider
) : ProviderWeakReference<IamController>() {

    override fun create(): IamController {
        return IamControllerImpl(iamRepositoryProvider.get())
    }
}