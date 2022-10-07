package com.reteno.di.provider

import com.reteno.data.remote.api.ApiClient
import com.reteno.data.remote.api.ApiClientImpl
import com.reteno.di.base.ProviderWeakReference

class ApiClientProvider : ProviderWeakReference<ApiClient>() {

    override fun create(): ApiClient {
        return ApiClientImpl()
    }
}