package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.lifecycle.RetenoActivityHelper

class RetenoActivityHelperProvider(
    private val eventControllerProvider: EventsControllerProvider
) :
    ProviderWeakReference<RetenoActivityHelper>() {

    override fun create(): RetenoActivityHelper {
        return RetenoActivityHelper(eventControllerProvider.get())
    }
}