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
    private val retenoDatabaseManagerProvider = RetenoDatabaseManagerProvider()

    val retenoActivityHelperProvider: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider()

    private val apiClientProvider: ApiClientProvider = ApiClientProvider()

    /** Repository **/
    val configRepositoryProvider: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
            sharedPrefsManagerProvider,
            restConfigProvider
        )
    private val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerProvider,
            configRepositoryProvider
        )

    private val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(
            apiClientProvider,
            configRepositoryProvider,
            retenoDatabaseManagerProvider
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(apiClientProvider, retenoDatabaseManagerProvider)
    val interactionControllerProvider: InteractionControllerProvider =
        InteractionControllerProvider(configRepositoryProvider, interactionRepositoryProvider)

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider)

    /** Controller **/
    val deeplinkControllerProvider: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)

    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProvider
        )

    val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    val scheduleControllerProvider: ScheduleControllerProvider =
        ScheduleControllerProvider(
            contactControllerProvider,
            interactionControllerProvider,
            eventsControllerProvider
        )

    init {

    }
}