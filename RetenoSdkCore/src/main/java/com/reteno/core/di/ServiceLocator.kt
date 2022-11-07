package com.reteno.core.di

import com.reteno.core.di.provider.*

class ServiceLocator {

    // TODO: Separate internal objects from externally exposed
    // TODO: Mark internal fields as internal

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider()

    private val deviceIdHelperProvider: DeviceIdHelperProvider =
        DeviceIdHelperProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdHelperProvider)

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
        ContactRepositoryProvider(apiClientProvider, restConfigProvider)
    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProvider
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(apiClientProvider)
    val interactionControllerProvider: InteractionControllerProvider =
        InteractionControllerProvider(configRepositoryProvider, interactionRepositoryProvider)

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider)
    val deeplinkControllerProvider: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)

    init {

    }
}