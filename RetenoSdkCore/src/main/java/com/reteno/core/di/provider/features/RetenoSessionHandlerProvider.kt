package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.controller.EventsControllerProvider
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.lifecycle.RetenoSessionHandlerImpl

internal class RetenoSessionHandlerProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val eventsControllerProvider: EventsControllerProvider
) : ProviderWeakReference<RetenoSessionHandler>() {

    override fun create(): RetenoSessionHandler {
        return RetenoSessionHandlerImpl(sharedPrefsManagerProvider.get(), eventsControllerProvider.get())
    }
}