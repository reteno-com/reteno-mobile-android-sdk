package com.reteno.core.di.provider.network

import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.data.remote.api.RestClientImpl
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.RestConfigProvider

internal class RestClientProvider(private val restConfigProvider: RestConfigProvider) :
    ProviderWeakReference<RestClient>() {

    override fun create(): RestClient {
        return RestClientImpl(restConfigProvider.get())
    }
}