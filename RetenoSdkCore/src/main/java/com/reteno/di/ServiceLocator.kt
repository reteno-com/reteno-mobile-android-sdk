package com.reteno.di

import com.reteno.di.provider.*

class ServiceLocator {

    val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider()

    val deviceIdProvider: DeviceIdProvider =
        DeviceIdProvider(sharedPrefsManagerProvider)
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