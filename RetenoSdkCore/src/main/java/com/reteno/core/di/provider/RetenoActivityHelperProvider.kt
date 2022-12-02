package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.controller.EventsControllerProvider
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoActivityHelperImpl

internal class RetenoActivityHelperProvider(
    private val eventControllerProvider: EventsControllerProvider
) :
    ProviderWeakReference<RetenoActivityHelper>() {

    override fun create(): RetenoActivityHelper {
        return RetenoActivityHelperImpl(eventControllerProvider.get())
    }
}