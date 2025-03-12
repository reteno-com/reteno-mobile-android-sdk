package com.reteno.core.di.provider

import com.reteno.core.RetenoConfig
import com.reteno.core.di.base.ProviderNewInstance

class RetenoConfigProvider(
    @Volatile
    private var config: RetenoConfig
) : ProviderNewInstance<RetenoConfig>() {

    override fun create(): RetenoConfig {
        return config
    }

    fun setConfig(config: RetenoConfig) {
        this.config = config
    }
}