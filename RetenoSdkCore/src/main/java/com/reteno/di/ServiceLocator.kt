package com.reteno.di

import android.content.Context
import com.reteno.di.provider.*

class ServiceLocator(applicationContext: Context) {

    val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider(applicationContext)

    val deviceIdProvider: DeviceIdProvider =
        DeviceIdProvider(applicationContext, sharedPrefsManagerProvider)
    val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdProvider)

    val retenoActivityHelperProvider: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider()

    val apiClientProvider: ApiClientProvider = ApiClientProvider()
    val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(apiClientProvider)
    val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(apiClientProvider)
    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(contactRepositoryProvider)

    init {

    }
}