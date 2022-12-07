package com.reteno.core.di.provider.database

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDeviceImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class RetenoDatabaseManagerDeviceProvider(private val retenoDatabaseProvider: DatabaseProvider) :
    ProviderWeakReference<RetenoDatabaseManagerDevice>() {

    override fun create(): RetenoDatabaseManagerDevice {
        return RetenoDatabaseManagerDeviceImpl(retenoDatabaseProvider.get())
    }
}