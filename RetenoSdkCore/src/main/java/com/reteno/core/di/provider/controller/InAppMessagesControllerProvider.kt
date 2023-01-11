package com.reteno.core.di.provider.controller

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.repository.InAppMessagesRepositoryProvider
import com.reteno.core.domain.controller.InAppMessagesController
import com.reteno.core.domain.controller.InAppMessagesControllerImpl

internal class InAppMessagesControllerProvider(
    private val inAppMessagesRepositoryProvider: InAppMessagesRepositoryProvider
) : ProviderWeakReference<InAppMessagesController>() {

    override fun create(): InAppMessagesController {
        return InAppMessagesControllerImpl(inAppMessagesRepositoryProvider.get())
    }
}