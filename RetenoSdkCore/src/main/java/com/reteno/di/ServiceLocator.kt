package com.reteno.di

import android.content.Context
import com.reteno.di.provider.ApiClientProvider
import com.reteno.di.provider.EventsControllerProvider
import com.reteno.di.provider.EventsRepositoryProvider
import com.reteno.di.provider.RetenoActivityHelperProvider

internal class ServiceLocator(applicationContext: Context) {

    internal val retenoActivityHelperProvider: RetenoActivityHelperProvider = RetenoActivityHelperProvider()

    internal val apiClientProvider: ApiClientProvider = ApiClientProvider()
    internal val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(apiClientProvider)
    internal val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    init {

    }
}