package com.reteno.core.di.provider

import com.reteno.core.RetenoConfig
import com.reteno.core.di.base.ProviderNewInstance

class RetenoConfigProvider : ProviderNewInstance<RetenoConfig>() {
    @Volatile
    private var config: RetenoConfig? = null

    override fun create(): RetenoConfig {
        return requireNotNull(config)
    }

    fun setConfig(config: RetenoConfig) {
        this.config = config
    }
}