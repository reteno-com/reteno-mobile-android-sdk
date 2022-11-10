package com.reteno.core.di.provider

import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiClientImpl
import com.reteno.core.di.base.ProviderWeakReference

class ApiClientProvider(private val restClientProvider: RestClientProvider) : ProviderWeakReference<ApiClient>() {

    override fun create(): ApiClient {
        return ApiClientImpl(restClientProvider.get())
    }
}