package com.reteno.core.internal.config

import com.reteno.core.RetenoConfig
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.util.Util

internal interface SdkConfiguration {
    val isDebug: Boolean

    val config: RetenoConfig?

    fun useUserConfig(config: RetenoConfig)

    fun useCachedConfig(config: RetenoConfig): Boolean
}

internal class SdkConfigurationImpl(
    private val lifecycleEventFilter: LifecycleEventFilter
) : SdkConfiguration {

    @Volatile
    private var userConfigInUse = false

    @Volatile
    override var isDebug: Boolean = false
        private set

    @Synchronized
    override fun useUserConfig(config: RetenoConfig) {
        userConfigInUse = true
        use(config)
    }

    @Synchronized
    override fun useCachedConfig(config: RetenoConfig): Boolean {
        if (userConfigInUse) return false
        use(config)
        return true
    }

    @Volatile
    override var config: RetenoConfig? = null
        private set

    private fun use(config: RetenoConfig) {
        this.config = config
        lifecycleEventFilter.update(config.lifecycleTrackingOptions)
        isDebug = config.isDebug
        Util.setIsDebug(config.isDebug)
    }
}
