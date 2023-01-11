package com.reteno.core.di.provider.features

import com.reteno.core.features.appinbox.AppInbox
import com.reteno.core.features.appinbox.AppInboxImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.controller.AppInboxControllerProvider

internal class AppInboxProvider(
    private val appInboxControllerProvider: AppInboxControllerProvider
) : ProviderWeakReference<AppInbox>() {

    override fun create(): AppInbox {
        return AppInboxImpl(appInboxControllerProvider.get())
    }
}