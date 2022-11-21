package com.reteno.core.di.provider

import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.di.base.ProviderWeakReference

class RestClientProvider(private val restConfigProvider: RestConfigProvider) :
    ProviderWeakReference<RestClient>() {

    override fun create(): RestClient {
        return RestClient(restConfigProvider.get())
    }
}