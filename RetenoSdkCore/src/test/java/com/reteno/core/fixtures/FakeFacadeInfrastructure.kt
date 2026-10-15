package com.reteno.core.fixtures

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.FacadeInfrastructure
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.mockk

internal class FakeSdkConfiguration : SdkConfiguration {
    override var isDebug: Boolean = false
    override var config: RetenoConfig? = null

    override fun useUserConfig(config: RetenoConfig) {
        this.config = config
    }

    override fun useCachedConfig(config: RetenoConfig): Boolean {
        this.config = config
        return true
    }
}

internal class FakeFacadeInfrastructure(private val fakeActions: FakeSdkActions) : FacadeInfrastructure {
    var actionsUsed = false
    override val actions: FakeSdkActions
        get() = fakeActions.also { actionsUsed = true }
    override val activityHelper = mockk<RetenoActivityHelper>(relaxed = true)
    override val configRepository = mockk<ConfigRepository>(relaxed = true)
    override val databaseManager = mockk<RetenoDatabaseManager>(relaxed = true)
    override val configuration = FakeSdkConfiguration()
}
