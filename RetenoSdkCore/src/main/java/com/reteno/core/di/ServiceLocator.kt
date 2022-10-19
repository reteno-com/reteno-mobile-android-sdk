package com.reteno.core.di

import com.reteno.core.di.provider.*

class ServiceLocator {

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider()

    private val deviceIdProvider: DeviceIdProvider =
        DeviceIdProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdProvider)

    val retenoActivityHelperProvider: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider()

    val configRepositoryProvider: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
            sharedPrefsManagerProvider,
            restConfigProvider
        )
    private val apiClientProvider: ApiClientProvider = ApiClientProvider()
    private val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(apiClientProvider)

    val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    private val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(apiClientProvider)
    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProvider
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(apiClientProvider)
    val interactionControllerProvider: InteractionControllerProvider =
        InteractionControllerProvider(configRepositoryProvider, interactionRepositoryProvider)

    init {

    }
}