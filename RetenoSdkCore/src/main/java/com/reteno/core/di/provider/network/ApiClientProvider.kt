package com.reteno.core.di.provider.network

import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiClientImpl
import com.reteno.core.di.base.ProviderWeakReference

internal class ApiClientProvider(private val restClientProvider: RestClientProvider) : ProviderWeakReference<ApiClient>() {

    override fun create(): ApiClient {
        return ApiClientImpl(restClientProvider.get())
    }
}