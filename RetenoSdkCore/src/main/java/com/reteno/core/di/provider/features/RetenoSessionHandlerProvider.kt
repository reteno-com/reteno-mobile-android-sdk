package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.lifecycle.RetenoSessionHandlerImpl

internal class RetenoSessionHandlerProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider
) : ProviderWeakReference<RetenoSessionHandler>() {

    override fun create(): RetenoSessionHandler {
        return RetenoSessionHandlerImpl(sharedPrefsManagerProvider.get())
    }
}