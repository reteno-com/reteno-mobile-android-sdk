package com.reteno.core.fixtures

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoConfig
import com.reteno.core.di.FacadeInfrastructure
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.permission.PermissionActivityDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

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

internal class FakeFacadeInfrastructure(
    fakeActions: FakeSdkActions,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) : FacadeInfrastructure {
    override val actions: FakeSdkActions = fakeActions
    override val configuration = FakeSdkConfiguration()
    override val permissions by lazy {
        PermissionActivityDelegate(ApplicationProvider.getApplicationContext(), mainDispatcher)
    }
}
