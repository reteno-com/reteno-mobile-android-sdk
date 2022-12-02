package com.reteno.core.di.provider.features

import com.reteno.core.appinbox.AppInbox
import com.reteno.core.appinbox.AppInboxImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.controller.AppInboxControllerProvider

internal class AppInboxProvider(
    private val appInboxControllerProvider: AppInboxControllerProvider
) : ProviderWeakReference<AppInbox>() {

    override fun create(): AppInbox {
        return AppInboxImpl(appInboxControllerProvider.get())
    }
}