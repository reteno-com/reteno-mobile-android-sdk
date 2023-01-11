package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.controller.InAppMessagesControllerProvider
import com.reteno.core.view.inapp.InAppMessagesView
import com.reteno.core.view.inapp.InAppMessagesViewImpl

internal class InAppMessagesViewProvider(
    private val retenoActivityHelperProvider: RetenoActivityHelperProvider,
    private val inAppMessagesViewProvider: InAppMessagesControllerProvider
) : ProviderWeakReference<InAppMessagesView>() {

    override fun create(): InAppMessagesView {
        return InAppMessagesViewImpl(retenoActivityHelperProvider.get(), inAppMessagesViewProvider.get())
    }
}